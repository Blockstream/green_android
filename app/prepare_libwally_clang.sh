#!/bin/bash

set -e
if [ -z "$JAVA_HOME" ]; then
    export JAVA_HOME=$JAVA7_HOME
fi
echo ${JAVA_HOME:?}
echo ${ANDROID_NDK:?}

NUM_JOBS=4
if [ -f /proc/cpuinfo ]; then
    NUM_JOBS=$(cat /proc/cpuinfo | grep ^processor | wc -l)
fi

if [ `uname` == "Darwin" ]; then
    export HOST_OS="x86_64-apple-darwin" # FIXME: Verify
else
    export HOST_OS="i686-linux-gnu"
fi

function build() {
    unset CFLAGS
    unset CPPFLAGS
    unset LDFLAGS
    configure_opts="--enable-silent-rules --disable-dependency-tracking --enable-swig-java --enable-endomorphism"

    case $1 in
        armeabi)
            arch=arm
            configure_opts="$configure_opts"
            export CFLAGS="-march=armv5te -mtune=xscale -msoft-float -mthumb"
            ;;
        armeabi-v7a)
            arch=arm
            configure_opts="$configure_opts" # FIXME: Fails to compile: --with-asm=arm
            export CFLAGS="-march=armv7-a -mfloat-abi=softfp -mfpu=neon -mthumb"
            export LDFLAGS="-Wl,--fix-cortex-a8"
            ;;
        arm64-v8a)
            arch=arm64
            configure_opts="$configure_opts" # FIXME: Fails to compile: --with-asm=arm
            export CFLAGS="-flax-vector-conversions"
            ;;
        mips)
            arch=mips
            # FIXME: Only needed until mips32r2 is not the default in clang
            export CFLAGS="-mips32"
            export LDLAGS="-mips32"
            ;;
        *)
            arch=$1
    esac

    export CFLAGS="$CFLAGS -O3" # Must  add optimisation flags for secp
    export CPPFLAGS="$CFLAGS"

    if [[ $arch == *"64"* ]]; then
        export ANDROID_VERSION="21"
    else
        export ANDROID_VERSION="14"
    fi

    rm -rf ./toolchain >/dev/null 2>&1
    $ANDROID_NDK/build/tools/make_standalone_toolchain.py --arch $arch --api $ANDROID_VERSION --install-dir=./toolchain

    echo '============================================================'
    echo Building $1
    echo '============================================================'
    ./configure --host=$HOST_OS --target=$arch $configure_opts >/dev/null
    make -o configure clean -j$NUM_JOBS >/dev/null 2>&1
    make -o configure -j$NUM_JOBS V=1

    mkdir -p ../src/main/jniLibs/$1
    toolchain/bin/*-strip -o ../src/main/jniLibs/$1/libwallycore.so src/.libs/libwallycore.so
}

if [ -n "$1" ]; then
    all_archs="$1"
else
    all_archs="armeabi armeabi-v7a arm64-v8a mips mips64 x86 x86_64"
fi

echo '============================================================'
echo 'Initialising build for architecture(s):'
echo $all_archs
echo '============================================================'
if [ -d libwally-core ]; then
    pushd libwally-core
    need_popd=yes
fi

tools/cleanup.sh
tools/autogen.sh

export PATH=`pwd`/toolchain/bin:$PATH
export CC=clang

for a in $all_archs; do
    build $a
done

# Note we can't do a full clean here since we need the generated Java files
rm -rf src/.libs ./toolchain

if [ -n "$need_popd" ]; then
    popd
fi
