#!/bin/bash
# vim: textwidth=0

BUILD_TYPE=release
#BUILD_TYPE=debug

DEVICE_ID="0123456789ABCDEF"
#DEVICE_ID="emulator-5554"
#CLEAN_REINSTALL=1

#TEST_API="false"
TEST_API="true"

KEYSTORE_DIRECTORY="../google-play-keystore"
KEYSTORE_SETTINGS="${KEYSTORE_DIRECTORY}/settings.sh"

ANDROID_SDK="/opt/android-sdk-update-manager"
ANDROID_API="android-14"
PROJECTNAME="smsnenado"
PACKAGENAME="com.sbar.${PROJECTNAME}"

SOURCE_ICON="assets/sms_spam_unsubscribed.png"

VERSION=$(egrep -o 'versionName="[0-9\.]*?"' AndroidManifest.xml |
          egrep -o '[0-9\.]*')
ADB_PATH="${ANDROID_SDK}/platform-tools/adb"

setup_environment() {
    echo "// automatically generated file

package com.sbar.${PROJECTNAME};

public class BuildEnv {
    public final static boolean TEST_API = ${TEST_API};
}" > src/com/sbar/${PROJECTNAME}/BuildEnv.java

    ctags -R .

    echo 'lines of code:'
    for i in '.*\.java$' '.*\.xml$'; do
        echo -n "$i "
        find . -regex $i -print0 -type f \
            | xargs -0 wc -l \
            | grep total \
            | awk '{ print $1 }'
    done

    mkdir libs/
    ln -s "${ANDROID_SDK}/extras/android/support/v4/android-support-v4.jar" libs/

    cp -rv "${ANDROID_SDK}/extras/google/google_play_services/libproject/google-play-services_lib" thirdparty/
    cd thirdparty/google-play-services_lib
    android update project \
        --name google-play-services \
        --path . \
        --target "${ANDROID_API}"
    cd ../..

    mkdir -p res/{drawable-hdpi,drawable-mdpi,drawable-ldpi}
    #convert "$SOURCE_ICON" -resize 72x72 res/drawable-hdpi/ic_launcher.png
    cp "$SOURCE_ICON" res/drawable-hdpi/ic_launcher.png
    convert "$SOURCE_ICON" -resize 48x48 res/drawable-mdpi/ic_launcher.png
    convert "$SOURCE_ICON" -resize 36x36 res/drawable-ldpi/ic_launcher.png

    #android create project --package "${PACKAGENAME}" --activity MainActivity \
    #   --name "${PROJECTNAME}" --path . --target "${ANDROID_API}"

    android update project \
        --name "${PROJECTNAME}" \
        --path . \
        --target "${ANDROID_API}" \
        --library thirdparty/google-play-services_lib
}


build_project() {
    #ant "-Djava.compilerargs=-Xlint:deprecation\ -Xlint:unchecked" ${BUILD_TYPE}
    ant "-Djava.compilerargs=-Xlint:unchecked" ${BUILD_TYPE}

    # sign and align
    if [[ "${BUILD_TYPE}" == "release" ]]; then
        if [ ! -f "${KEYSTORE_SETTINGS}" ]; then
            echo
            echo
            echo "FAIL: cannot find settings.sh for keystore"
            echo
            echo "To be able to sign your builds please read"
            echo "http://developer.android.com/tools/publishing/app-signing.html"
            echo
            echo "Then create your keystore file like that:"
            echo
            echo "${PROJECTNAME} \$ mkdir ${KEYSTORE_DIRECTORY}"
            echo "${PROJECTNAME} \$ cd ${KEYSTORE_DIRECTORY}"
            echo "keytool -genkey -v \\"
            echo "        -keystore 'filename.keystore' \\"
            echo "        -alias 'alias' \\"
            echo "        -keyalg RSA \\"
            echo "        -keysize 2048 \\"
            echo "        -validity 10000 \\"
            echo "        -noprompt \\"
            echo "        -storepass 'password'"
            echo
            echo "After it create settings.sh with password, alias and filename"
            echo "like that:"
            echo
            echo "${PROJECTNAME} \$ cat > ${KEYSTORE_SETTINGS}"
            echo "PSW='password'"
            echo "KEYSTORE_ALIAS_NAME='alias'"
            echo "KEYSTORE_FILE='filename.keystore'"
            exit 1
        fi
        source "${KEYSTORE_SETTINGS}"
        KEYSTORE="../google-play-keystore/${KEYSTORE_FILE}"

        jarsigner -verbose \
                  -sigalg SHA1withRSA \
                  -digestalg SHA1 \
                  -keystore "${KEYSTORE}" \
                  -storepass "${PSW}" \
                  -keypass "${PSW}" \
                  bin/*-unsigned.apk "${KEYSTORE_ALIAS_NAME}"

        jarsigner -verify bin/*-unsigned.apk

        zipalign -v 4 bin/*-unsigned.apk "bin/${PROJECTNAME}-${BUILD_TYPE}.apk"
    fi

    # copy the final file
    cp -fv bin/${PROJECTNAME}-${BUILD_TYPE}.apk \
           builds/${PROJECTNAME}-${BUILD_TYPE}-${VERSION}.apk
}


prepare_adb() {
    init_script="/etc/init.d/adb"
    if [[ ! $(ps uax | grep 'adb -P' | grep -v grep) ]]; then
        echo 'running adb from root (by canceling it will run from current user)'
        if [ -f ${init_script} ]; then
            sudo /etc/init.d/adb restart
        else
            sudo ${ADB_PATH} start-server
        fi
    fi
}


install() {
    #webput bin/${PROJECTNAME}-${BUILD_TYPE}.apk b.apk

    if [[ ${CLEAN_REINSTALL} ]]; then
        ${ADB_PATH} -s "${DEVICE_ID}" uninstall "$PACKAGENAME"
    fi

    ${ADB_PATH} -s "${DEVICE_ID}" install -r bin/*-${BUILD_TYPE}.apk
    #adb reboot
}


wake_phone() {
    screen_on=$(${ADB_PATH} -s "${DEVICE_ID}"  shell dumpsys power |\
        grep mScreenOn | \
        cut -d'=' -f2 | \
        grep 'true'
    )

    if [[ ! ${screen_on} ]]; then
        echo "waking up the device's screen"
        ${ADB_PATH} -s "${DEVICE_ID}" shell input keyevent KEYCODE_POWER
    fi
}


# main
set -e
./clear.sh
setup_environment
build_project
set +e
prepare_adb
install
wake_phone
