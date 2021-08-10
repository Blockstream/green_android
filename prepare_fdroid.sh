#!/bin/bash

set -e
if [ -z "$ANDROID_NDK" ]; then
    export ANDROID_NDK=$(dirname `which ndk-build 2>/dev/null`)
fi

echo ${ANDROID_NDK:?}
exec crypto/prepare_gdk_clang.sh $1
