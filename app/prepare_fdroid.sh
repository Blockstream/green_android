#!/bin/bash

set -e
if [ -z "$ANDROID_NDK" ]; then
    export ANDROID_NDK=$(dirname `which ndk-build 2>/dev/null`)
fi

echo ${ANDROID_NDK:?}
exec ./prepare_libwally_clang.sh
