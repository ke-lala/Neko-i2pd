#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SRC="$ROOT/third_party/hev-socks5-tunnel/source"
NDK="${ANDROID_NDK_HOME:-${ANDROID_HOME:?请设置 ANDROID_HOME}/ndk/29.0.14206865}"
OUT="${1:-$ROOT/app/libs/hev-socks5-tunnel-2.19.3.aar}"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

[[ -x "$NDK/ndk-build" ]] || { echo "找不到 ndk-build：$NDK" >&2; exit 1; }
[[ -f "$SRC/Android.mk" && -f "$SRC/android/hev/htproxy/TProxyService.java" ]] || {
  echo 'Hev 2.19.3 源码不完整' >&2
  exit 1
}

# Upstream's prebuilt AAR declares API 29. Build from the pinned source at API 23 instead;
# this checks the native code against Android 6 symbols and keeps this app usable on Android 8.
"$NDK/ndk-build" \
  NDK_PROJECT_PATH="$SRC" \
  APP_BUILD_SCRIPT="$SRC/Android.mk" \
  NDK_APPLICATION_MK="$SRC/Application.mk" \
  APP_PLATFORM=android-23 \
  APP_ABI='armeabi-v7a arm64-v8a x86 x86_64' \
  NDK_OUT="$WORK/obj" \
  -j"${GRADLE_WORKERS:-2}" hev-socks5-tunnel

mkdir -p "$WORK/aar/classes" "$WORK/aar/jni" "$(dirname "$OUT")"
javac --release 8 -d "$WORK/aar/classes" "$SRC/android/hev/htproxy/TProxyService.java"
jar cf "$WORK/aar/classes.jar" -C "$WORK/aar/classes" .
cat > "$WORK/aar/AndroidManifest.xml" <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="hev.socks5.tunnel">
    <uses-sdk android:minSdkVersion="23" />
</manifest>
EOF
printf '%s\n' '-keep class hev.htproxy.** { *; }' > "$WORK/aar/proguard.txt"
for abi in armeabi-v7a arm64-v8a x86 x86_64; do
  lib="$WORK/obj/local/$abi/libhev-socks5-tunnel.so"
  [[ -f "$lib" ]] || { echo "缺少 ABI 产物：$lib" >&2; exit 1; }
  mkdir -p "$WORK/aar/jni/$abi"
  cp "$lib" "$WORK/aar/jni/$abi/libhev-socks5-tunnel.so"
done
rm -f "$OUT"
(
  cd "$WORK/aar"
  zip -q -r "$OUT" .
)
echo "生成：$OUT"
sha256sum "$OUT"
