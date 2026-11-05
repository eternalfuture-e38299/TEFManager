#!/usr/bin/env bash
# =============================================================================
# build_zygisk.sh —— 构建 TEFManager 的 Zygisk 模块包
#
# 产物：
#   dist/TEFManager-Zygisk-v<version>.zip
#
# 依赖：
#   ANDROID_NDK_HOME（或 ANDROID_NDK_ROOT）指向 NDK r26+
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
SRC="$ROOT/zygisk-module"
DIST="$ROOT/dist"
BUILD="$ROOT/build/zygisk"
NDK="${ANDROID_NDK_HOME:-${ANDROID_NDK_ROOT:-}}"
API=24

if [ -z "$NDK" ] || [ ! -d "$NDK" ]; then
    echo "error: ANDROID_NDK_HOME / ANDROID_NDK_ROOT is not set" >&2
    exit 1
fi

VERSION="$(sed -n 's/.*const val VERSION_NAME[[:space:]]*:[[:space:]]*String[[:space:]]*=[[:space:]]*"\([^"]*\)".*/\1/p' \
    "$ROOT/composeApp/build.gradle.kts" | head -1)"
VERSION_CODE="$(sed -n 's/.*const val VERSION_CODE[[:space:]]*:[[:space:]]*Int[[:space:]]*=[[:space:]]*\([0-9]*\).*/\1/p' \
    "$ROOT/composeApp/build.gradle.kts" | head -1)"
[ -n "$VERSION" ] || VERSION=0.0.0
[ -n "$VERSION_CODE" ] || VERSION_CODE=0

ABIS=(arm64-v8a armeabi-v7a x86_64 x86)
STAGE="$BUILD/stage"
rm -rf "$BUILD" "$STAGE"
mkdir -p "$STAGE/zygisk" "$DIST"

for ABI in "${ABIS[@]}"; do
    echo "==> building $ABI"
    cmake -S "$SRC" -B "$BUILD/$ABI" \
        -DCMAKE_TOOLCHAIN_FILE="$NDK/build/cmake/android.toolchain.cmake" \
        -DANDROID_ABI="$ABI" \
        -DANDROID_PLATFORM="android-$API" \
        -DCMAKE_BUILD_TYPE=Release >/dev/null
    cmake --build "$BUILD/$ABI" -j"$(nproc)" >/dev/null
    cp "$BUILD/$ABI/libtefzygisk.so" "$STAGE/zygisk/$ABI.so"
done

cp "$SRC/module/module.prop" "$STAGE/module.prop"
cp "$SRC/module/customize.sh" "$STAGE/customize.sh"
cp "$SRC/module/service.sh" "$STAGE/service.sh"
cp "$SRC/module/uninstall.sh" "$STAGE/uninstall.sh"
sed -i "s/^version=.*/version=v$VERSION/" "$STAGE/module.prop"
sed -i "s/^versionCode=.*/versionCode=$VERSION_CODE/" "$STAGE/module.prop"
chmod 0755 "$STAGE/customize.sh" "$STAGE/service.sh" "$STAGE/uninstall.sh"

ZIP="$DIST/TEFManager-Zygisk-v$VERSION.zip"
rm -f "$ZIP"
( cd "$STAGE" && zip -qrX "$ZIP" . )
echo "done: $ZIP"
unzip -l "$ZIP"
