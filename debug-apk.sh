set -x
clear
./gradlew assembleDebug
adb kill-server
adb start-server
sleep 2 # Give ADB a moment to recognize the device
adb install -r -t -g /mnt/win/Users/MkReman/Gdrive/projects/Finance-Tracker/app/build/outputs/apk/debug/app-debug.apk
