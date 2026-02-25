#!/bin/bash

./gradlew assembleRelease

~/Android/Sdk/build-tools/34.0.0/apksigner sign --ks ~/.android/debug.keystore --ks-pass pass:android --key-pass pass:android --out /mnt/win/Users/MkReman/Gdrive/projects/Finance-Tracker/app/build/outputs/apk/release/app-release-signed.apk /mnt/win/Users/MkReman/Gdrive/projects/Finance-Tracker/app/build/outputs/apk/release/app-release-unsigned.apk
 

adb install -r /mnt/win/Users/MkReman/Gdrive/projects/Finance-Tracker/app/build/outputs/apk/release/app-release-signed.apk
