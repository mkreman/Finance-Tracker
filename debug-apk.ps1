$ErrorActionPreference = "Stop"

$windowsJava = Get-Command java -ErrorAction SilentlyContinue
if ($windowsJava) {
    & "$PSScriptRoot\gradlew.bat" assembleDebug
} else {
    $wslProjectPath = "/mnt/" + $PSScriptRoot.Substring(0, 1).ToLower() + $PSScriptRoot.Substring(2).Replace("\", "/")
    & wsl.exe --cd $wslProjectPath ./gradlew assembleDebug
}
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

& adb kill-server
& adb start-server
Start-Sleep -Seconds 2

$apkPath = Join-Path $PSScriptRoot "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $apkPath)) {
    throw "Debug APK was not created: $apkPath"
}

& adb install -r -t -g $apkPath
if ($LASTEXITCODE -ne 0) {
    Write-Error "APK installation failed. The installed app and APK may be signed with different keys; keeping the installed app preserves its data."
    exit $LASTEXITCODE
}

# powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\debug-apk.ps1