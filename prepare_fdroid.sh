#!/bin/bash

set -e
if [ -z "$ANDROID_NDK" ]; then
    export ANDROID_NDK=$(dirname `which ndk-build 2>/dev/null`)
fi

echo ${ANDROID_NDK:?}
exec gdk/prepare_gdk_clang.sh $1

echo "Changing Breez SDK dependency from KMP to Android"
sed -i -e 's/api(libs.breez.sdk.kmp)/\/\/ api(libs.breez.sdk.kmp)/g' common/build.gradle.kts
sed -i -e 's/\/\/ api(libs.breez.sdk.android)/api(libs.breez.sdk.android)/g' common/build.gradle.kts