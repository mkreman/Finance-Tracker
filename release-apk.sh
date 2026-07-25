#!/bin/bash

set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
apk_dir="$project_dir/app/build/outputs/apk/release"
unsigned_apk="$apk_dir/app-release-unsigned.apk"
signed_apk="$apk_dir/app-release-signed.apk"

bash "$project_dir/gradlew" assembleRelease

if [ ! -f "$unsigned_apk" ]; then
	echo "Missing release APK: $unsigned_apk" >&2
	exit 1
fi

"$HOME/Android/Sdk/build-tools/34.0.0/apksigner" sign \
	--ks "$HOME/.android/debug.keystore" \
	--ks-pass pass:android \
	--key-pass pass:android \
	--out "$signed_apk" \
	"$unsigned_apk"

adb install -r "$signed_apk"
