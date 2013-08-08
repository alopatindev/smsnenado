#!/bin/bash

set -e

ANDROID_SDK="/opt/android-sdk-update-manager"

#android create project --package com.sbar.smsnenado --activity MainActivity --name smsnenado --path . --target "android-15"
android update project --name smsnenado --path . --target "android-15"

mkdir libs/
ln -s $ANDROID_SDK/extras/android/support/v4/android-support-v4.jar libs/

ant debug
adb install -r bin/*-debug.apk
#adb reboot
