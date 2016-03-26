#!/bin/bash

set -e

if [ `uname` == "Darwin" ]; then
    export SYSTEM="darwin-x86_64"
elif [ `uname -m` = "x86_64" ]; then
    export SYSTEM="linux-x86_64"
else
    export SYSTEM="linux-x86"
fi

./autogen.sh

for ARCH in arm-linux-androideabi mipsel-linux-android x86; do
    export PATH=$ANDROID_NDK/toolchains/$ARCH-4.8/prebuilt/$SYSTEM/bin:$PATH

    export ARCH_SHORT=`echo $ARCH | cut -d'-' -f 1 | sed s/mipsel/mips/` # arm|mips|x86
    export ARCH=`echo $ARCH | sed s/x86/i686-linux-android/`

    export SYSROOT=$ANDROID_NDK/platforms/android-9/arch-$ARCH_SHORT/
    export CC=$ARCH-gcc
    export CPP=$ARCH-cpp
    export CPPFLAGS=--sysroot=$SYSROOT
    export CFLAGS=--sysroot=$SYSROOT

    ./configure --host=$ARCH --target=$ARCH --enable-jni --enable-experimental --enable-module-schnorr --enable-module-ecdh
    make -o configure clean
    make -o configure
    ARCHDIR=output/`echo $ARCH_SHORT | sed s/arm/armeabi/` # armeabi|mips|x86
    mkdir -p $ARCHDIR
    cp .libs/libsecp256k1.so $ARCHDIR
done

cd ..
