#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
: "${ANDROID_HOME:?请设置 ANDROID_HOME 为 Android SDK 目录}"
export ANDROID_NDK_HOME="${ANDROID_NDK_HOME:-$ANDROID_HOME/ndk/29.0.14206865}"
if [[ -n "${JAVA_HOME:-}" ]]; then export PATH="$JAVA_HOME/bin:$PATH"; fi
command -v javac >/dev/null || { echo '需要完整 JDK 17+：未找到 javac'; exit 1; }
[[ -x "$ANDROID_NDK_HOME/ndk-build" ]] || { echo "NDK 不存在：$ANDROID_NDK_HOME"; exit 1; }
[[ -f binary/jni/i2pd/libi2pd/RouterContext.cpp ]] || { echo '缺少 i2pd 子模块源码，请解压完整源码 ZIP 或 git submodule update --init --recursive'; exit 1; }
for asset_dir in certificates/family certificates/reseed torrents/web; do
  asset_path="app/src/main/assets/$asset_dir"
  [[ -d "$asset_path" ]] && find "$asset_path" -type f -print -quit | grep -q . || {
    echo "缺少运行必需的 APK 资源：$asset_path"
    exit 1
  }
done
printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties
chmod +x gradlew binary/jni/*.sh
binary/jni/build.sh
./gradlew --max-workers="${GRADLE_WORKERS:-2}" assembleDebug "$@"
echo 'APK 输出：app/build/outputs/apk/debug/'
