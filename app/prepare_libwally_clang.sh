#!/bin/bash

set -e
if [ -z "$JAVA_HOME" ]; then
    export JAVA_HOME=$JAVA7_HOME
fi
echo ${JAVA_HOME:?}
echo ${ANDROID_NDK:?}

if [ -d libwally-core ]; then
    cd libwally-core
else
    git clone https://github.com/ElementsProject/libwally-core.git
    cd libwally-core
    git checkout tags/release_0.6.1 -b release_0.6.1
fi

source ./tools/android_helpers.sh

# FIXME: sed only needed until wally 0.6.1 is released
all_archs=$(android_get_arch_list | sed 's/mips mips64 //g' | sed 's/armeabi //g')
if [ -n "$1" ]; then
    all_archs="$1"
fi

echo '============================================================'
echo 'Initialising build for architecture(s):'
echo $all_archs
echo '============================================================'
tools/cleanup.sh
tools/autogen.sh

for arch in $all_archs; do
    echo '============================================================'
    echo Building $arch
    echo '============================================================'
    # Use API level 14 for non-64 bit targets for better device coverage
    api="14"
    if [[ $arch == *"64"* ]]; then
        api="21"
    fi
    opts="--disable-dependency-tracking --enable-swig-java --disable-swig-python"

    rm -rf ./toolchain >/dev/null 2>&1
    android_build_wally $arch $PWD/toolchain $api "$opts"
    mkdir -p ../src/main/jniLibs/$arch
    toolchain/bin/*-strip -o ../src/main/jniLibs/$arch/libwallycore.so src/.libs/libwallycore.so
done

# Note we can't do a full clean here since we need the generated Java files
rm -rf src/.libs ./toolchain
