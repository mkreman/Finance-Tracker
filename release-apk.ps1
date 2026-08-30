$ErrorActionPreference = "Stop"

$wslProjectPath = "/mnt/" + $PSScriptRoot.Substring(0, 1).ToLower() + $PSScriptRoot.Substring(2).Replace("\", "/")
$unsignedApk = Join-Path $PSScriptRoot "app\build\outputs\apk\release\app-release-unsigned.apk"
$signedApk = Join-Path $PSScriptRoot "app\build\outputs\apk\release\app-release-signed.apk"

& wsl.exe --cd $wslProjectPath ./gradlew assembleRelease
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

if (-not (Test-Path $unsignedApk)) {
    throw "Missing release APK: $unsignedApk"
}

& wsl.exe --cd $wslProjectPath bash -lc '"$HOME/Android/Sdk/build-tools/34.0.0/apksigner" sign --ks "$HOME/.android/debug.keystore" --ks-pass pass:android --key-pass pass:android --out "app/build/outputs/apk/release/app-release-signed.apk" "app/build/outputs/apk/release/app-release-unsigned.apk"'
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

if (-not (Test-Path $signedApk)) {
    throw "Missing signed release APK: $signedApk"
}

& adb install -r -g $signedApk
if ($LASTEXITCODE -ne 0) {
    Write-Error "APK installation failed. The installed app and APK may be signed with different keys; keeping the installed app preserves its data."
    exit $LASTEXITCODE
}

# powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\release-apk.ps1