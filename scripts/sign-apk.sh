#!/usr/bin/env bash
# Signs an unsigned release APK with the release key (zipalign + apksigner from the Android SDK).
# Works for any tag, because the build itself needs no signing config.
#
# Usage: scripts/sign-apk.sh <app-release-unsigned.apk> <output.apk>
#
# Key material comes from RELEASE_KEYSTORE_FILE, RELEASE_KEYSTORE_PASSWORD, RELEASE_KEY_ALIAS and
# RELEASE_KEY_PASSWORD (CI). When RELEASE_KEYSTORE_FILE is unset, it is read from the properties
# file in RELEASE_SIGNING_PROPERTIES (storeFile, storePassword, keyAlias, keyPassword), by default
# ~/.android/interval-timer-release.properties (local builds).
set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "usage: $0 <unsigned.apk> <output.apk>" >&2
  exit 2
fi
in=$1
out=$2

if [[ -z "${RELEASE_KEYSTORE_FILE:-}" ]]; then
  props=${RELEASE_SIGNING_PROPERTIES:-$HOME/.android/interval-timer-release.properties}
  prop() { sed -n "s/^$1=//p" "$props"; }
  RELEASE_KEYSTORE_FILE=$(prop storeFile)
  RELEASE_KEYSTORE_PASSWORD=$(prop storePassword)
  RELEASE_KEY_ALIAS=$(prop keyAlias)
  RELEASE_KEY_PASSWORD=$(prop keyPassword)
  export RELEASE_KEYSTORE_PASSWORD RELEASE_KEY_PASSWORD
fi

sdk=${ANDROID_HOME:-${ANDROID_SDK_ROOT:?set ANDROID_HOME to the Android SDK}}
build_tools=$(ls -d "$sdk"/build-tools/* | sort -V | tail -1)
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

# 16 KB page alignment for the bundled native library (Android 15+ devices).
"$build_tools/zipalign" -P 16 -f 4 "$in" "$tmp/aligned.apk"
"$build_tools/apksigner" sign \
  --ks "$RELEASE_KEYSTORE_FILE" \
  --ks-pass env:RELEASE_KEYSTORE_PASSWORD \
  --ks-key-alias "$RELEASE_KEY_ALIAS" \
  --key-pass env:RELEASE_KEY_PASSWORD \
  --v4-signing-enabled false \
  --out "$out" \
  "$tmp/aligned.apk"
"$build_tools/apksigner" verify --print-certs "$out" | grep -E "certificate (DN|SHA-256)"
