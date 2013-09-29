#!/bin/bash

PID=$(adb shell 'ps | grep smsnenado' | awk '{print $2}')
[[ $PID -eq '' ]] || adb shell kill $PID

adb shell am start -n com.sbar.smsnenado/.MainActivity

adb forward --remove-all
sleep 2s
PORT=$(adb jdwp | tail -n1)
adb forward tcp:8003 jwdp:$PORT
jdb -attach localhost:8700 -sourcepath ./src
