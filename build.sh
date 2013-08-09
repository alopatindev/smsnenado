#!/bin/bash

set -e

SOURCE_ICON="assets/sms_spam_unsubscribed.png"
ANDROID_SDK="/opt/android-sdk-update-manager"

#android create project --package com.sbar.smsnenado --activity MainActivity --name smsnenado --path . --target "android-15"
android update project --name smsnenado --path . --target "android-15"

mkdir -p res/{drawable-hdpi,drawable-mdpi,drawable-ldpi}
convert "$SOURCE_ICON" -resize 72x72 res/drawable-hdpi/ic_launcher.png
convert "$SOURCE_ICON" -resize 48x48 res/drawable-mdpi/ic_launcher.png
convert "$SOURCE_ICON" -resize 36x36 res/drawable-ldpi/ic_launcher.png

mkdir libs/
ln -s $ANDROID_SDK/extras/android/support/v4/android-support-v4.jar libs/

ant debug

set +e
#adb -s 0123456789ABCDEF install -r bin/*-debug.apk
adb -s emulator-5554 install -r bin/*-debug.apk
#adb reboot
