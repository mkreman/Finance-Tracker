set -x
./gradlew assembleDebug
adb kill-server
adb start-server
sleep 5 # Give ADB a moment to recognize the device
adb install -r -t -g /home/mkreman/projects/Finance-Tracker/app/build/outputs/apk/debug/app-debug.apk
