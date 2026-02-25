# Finance Tracker

A comprehensive Android application for personal finance management, built with Kotlin and Jetpack Compose.

## Overview

Finance Tracker is a feature-rich personal finance management app that helps you track income, expenses, budgets, and transfers. It provides detailed insights into your financial activities with a user-friendly interface.

## Features

### Core Functionality
- **Transaction Management**: Track income, expenses, and transfers with date and category support
- **Accounts**: Manage multiple accounts with customizable icons and settings
- **Budget Tracking**: Set and monitor budgets across different categories
- **Dashboard**: Visualize your financial overview with summaries
- **Biometric Security**: Secure your financial data with fingerprint authentication
- **Data Export/Import**: Export and import transactions in TSV format
- **Home Widget**: Quick access widget for your Finance Tracker data

### Current Version: 0.5.4

## Requirements

### Development
- **Android Studio** (latest version recommended)
- **Java Development Kit (JDK)** 17 or higher
- **Gradle** 8.0 or higher (automatically managed by Gradle wrapper)

### Target & Minimum SDK
- **Minimum SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 15 (API 35)
- **Compile SDK**: Android 15 (API 35)

### For Installation via ADB
- **Android Device** or emulator running Android 8.0 or higher
- **USB Debugging** enabled on the device
- **ADB (Android Debug Bridge)** installed on your computer

## Building the Application

### 1. Clone/Download the Project
```bash
cd /path/to/Finance-Tracker
```

### 2. Build the APK

#### Debug Build (Development)
```bash
./gradlew build
```
Or specifically for debug APK:
```bash
./gradlew assembleDebug
```

The debug APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

#### Release Build (Production)
```bash
./gradlew assembleRelease
```

The release APK will be located at:
```
app/build/outputs/apk/release/app-release.apk
```

## Installing the Application via ADB

### Prerequisites
1. Enable **USB Debugging** on your Android device:
   - Go to **Settings** → **Developer Options**
   - Enable **USB Debugging**

2. Connect your device via USB cable

3. Verify device is connected:
   ```bash
   adb devices
   ```
   You should see your device listed.

### Installation Steps

#### Install Debug APK
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

#### Install Release APK
Sign it first using `apksigner`
```bash
~/Android/Sdk/build-tools/34.0.0/apksigner sign --ks ~/.android/debug.keystore --ks-pass pass:android --key-pass pass:android /mnt/win/Users/MkReman/Gdrive/projects/Finance-Tracker/app/build/outputs/apk/release/app-release-unsigned.apk
```
And then install it

```bash
adb install app/build/outputs/apk/release/app-release.apk
```

#### Uninstall the App
```bash
adb uninstall com.moneytracker.app
```

#### Reinstall with Force Replacement
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

#### Install and Launch Immediately
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk && adb shell am start -n com.moneytracker.app/com.moneytracker.app.MainActivity
```

## Build Variants

The project uses the following build configuration:

- **compileSdk**: 35 (Android 15)
- **targetSdk**: 35 (Android 15)
- **minSdk**: 26 (Android 8.0)
- **Java Version**: 17

## Project Structure

```
Finance-Tracker/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── kotlin/          # Kotlin source code
│   │       └── AndroidManifest.xml
│   ├── build.gradle.kts         # App-level build configuration
│   └── proguard-rules.pro       # ProGuard rules for release build
├── build.gradle.kts             # Root-level build configuration
├── settings.gradle.kts          # Gradle settings
├── gradle.properties            # Gradle properties
└── README.md                    # This file
```

## Key Dependencies

- **Jetpack Compose**: UI framework
- **Room Database**: Local data persistence
- **Hilt**: Dependency injection
- **Navigation Compose**: App navigation
- **WorkManager**: Background task scheduling
- **DataStore**: Preferences management
- **Biometric**: Biometric authentication
- **Glance**: Home widget support

## Troubleshooting

### ADB Device Not Found
```bash
# List connected devices
adb devices

# If no device appears, try:
adb kill-server
adb start-server
adb devices
```

### Installation Fails with "INSTALL_FAILED_VERSION_DOWNGRADE"
```bash
# Uninstall the existing app first
adb uninstall com.moneytracker.app
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Build Fails
```bash
# Clean and rebuild
./gradlew clean build
```

### Device Not Recognized (Linux/Mac)
For Linux users, you may need to set up USB device permissions:
```bash
sudo usermod -a -G plugdev $USER
```

## Application Permissions

The app requires the following permissions (please check AndroidManifest.xml for details):
- **Storage**: For importing/exporting transaction data
- **Biometric**: For fingerprint authentication

## Development Notes

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: Clean Architecture with MVVM
- **Database**: Room Database
- **Dependency Injection**: Hilt
- **Compilation**: Kotlin 1.5.9 with JVM target 17

## Version History

### Current Version: 0.5.4 (Latest)
See `Versions.txt` for detailed feature roadmap and upcoming improvements.

## License

[Add your license information here]

## Support

For issues, questions, or feature requests, please refer to the application's issue tracker or contact the development team.

---

**Last Updated**: February 2026
**Application ID**: com.moneytracker.app
