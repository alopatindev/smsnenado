#!/bin/bash

set -e

# 1. Setting up environment
./clear.sh

BUILD_TYPE=release
#BUILD_TYPE=debug

#TEST_API="false"
TEST_API="true"

KEYSTORE_DIRECTORY="../google-play-keystore"
KEYSTORE_SETTINGS="$KEYSTORE_DIRECTORY/settings.sh"

ANDROID_SDK="/opt/android-sdk-update-manager"
PROJECTNAME="smsnenado"
PACKAGENAME="com.sbar.${PROJECTNAME}"

SOURCE_ICON="assets/sms_spam_unsubscribed.png"

VERSION=$(egrep -o 'versionName="[0-9\.]*?"' AndroidManifest.xml |
          egrep -o '[0-9\.]*')

echo "// automatically generated file

package com.sbar.smsnenado;

public class BuildEnv {
    public final static boolean TEST_API = ${TEST_API};
}" > src/com/sbar/smsnenado/BuildEnv.java

ctags -R .

echo 'lines of code:'
for i in '.*\.java$' '.*\.xml$'; do
    echo -n "$i "
    find . -regex $i -print0 -type f \
        | xargs -0 wc -l \
        | grep total \
        | awk '{ print $1 }'
done

#android create project --package "${PACKAGENAME}" --activity MainActivity \
#   --name smsnenado --path . --target "android-14"

android update project --name smsnenado --path . --target "android-14"

mkdir -p res/{drawable-hdpi,drawable-mdpi,drawable-ldpi}
#convert "$SOURCE_ICON" -resize 72x72 res/drawable-hdpi/ic_launcher.png
cp "$SOURCE_ICON" res/drawable-hdpi/ic_launcher.png
convert "$SOURCE_ICON" -resize 48x48 res/drawable-mdpi/ic_launcher.png
convert "$SOURCE_ICON" -resize 36x36 res/drawable-ldpi/ic_launcher.png

mkdir libs/
ln -s $ANDROID_SDK/extras/android/support/v4/android-support-v4.jar libs/

# 2. Building
ant "-Djava.compilerargs=-Xlint:deprecation\ -Xlint:unchecked" ${BUILD_TYPE}

# 3. Build signing and align
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

# 4. Installation
set +e
cp -fv bin/smsnenado-${BUILD_TYPE}.apk \
       builds/smsnenado-${BUILD_TYPE}-${VERSION}.apk

webput bin/smsnenado-${BUILD_TYPE}.apk b.apk

#adb -s 0123456789ABCDEF uninstall "$PACKAGENAME"
adb -s 0123456789ABCDEF install -r bin/*-${BUILD_TYPE}.apk

#adb -s emulator-5554 uninstall "$PACKAGENAME"
#adb -s emulator-5554 install -r bin/*-${BUILD_TYPE}.apk
#adb reboot
