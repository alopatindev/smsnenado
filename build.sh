#!/bin/bash

set -e

./clear.sh

SOURCE_ICON="assets/sms_spam_unsubscribed.png"
ANDROID_SDK="/opt/android-sdk-update-manager"
PROJECTNAME="smsnenado"
PACKAGENAME="com.sbar.${PROJECTNAME}"

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
#   --name smsnenado --path . --target "android-15"

android update project --name smsnenado --path . --target "android-15"

mkdir -p res/{drawable-hdpi,drawable-mdpi,drawable-ldpi}
convert "$SOURCE_ICON" -resize 72x72 res/drawable-hdpi/ic_launcher.png
convert "$SOURCE_ICON" -resize 48x48 res/drawable-mdpi/ic_launcher.png
convert "$SOURCE_ICON" -resize 36x36 res/drawable-ldpi/ic_launcher.png

mkdir libs/
ln -s $ANDROID_SDK/extras/android/support/v4/android-support-v4.jar libs/

ant debug

set +e
webput bin/smsnenado-debug.apk b.apk

#adb -s 0123456789ABCDEF uninstall "$PACKAGENAME"
adb -s 0123456789ABCDEF install -r bin/*-debug.apk

#adb -s emulator-5554 uninstall "$PACKAGENAME"
#adb -s emulator-5554 install -r bin/*-debug.apk
#adb reboot
