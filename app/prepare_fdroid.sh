#!/bin/bash

set -e


cd scrypt
patch -p1 < ../scrypt_Makefile.patch

if [ `uname` == "Darwin" ]; then
    export SYSTEM="darwin-x86_64"
elif [ `uname -m` = "x86_64" ]; then
    export SYSTEM="linux-x86_64"
else
    export SYSTEM="linux-x86"
fi

for ARCH in arm-linux-androideabi mipsel-linux-android x86; do
    export ARCH_SHORT=`echo $ARCH | cut -d'-' -f 1 | sed s/mipsel/mips/` # arm|mips|x86
    export PATH=$ANDROID_NDK/toolchains/$ARCH-4.9/prebuilt/$SYSTEM/bin:$PATH
    make clean TARGET=android
    make NDK_ROOT=$ANDROID_NDK TARGET=android ARCH_SHORT=$ARCH_SHORT ARCH=`echo $ARCH | sed s/x86/i686-linux-android/`
    ARCHDIR=../src/main/jniLibs/`echo $ARCH_SHORT | sed s/arm/armeabi/` # armeabi|mips|x86
    mkdir -p $ARCHDIR
    cp target/libscrypt.so $ARCHDIR
    $ANDROID_NDK/toolchains/$ARCH-4.9/prebuilt/$SYSTEM/bin/`echo $ARCH | sed s/x86/i686-linux-android/`-strip $ARCHDIR/libscrypt.so
done

cd ../secp256k1

./autogen.sh

for ARCH in arm-linux-androideabi mipsel-linux-android x86; do
    export PATH=$ANDROID_NDK/toolchains/$ARCH-4.9/prebuilt/$SYSTEM/bin:$PATH

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
    $ANDROID_NDK/toolchains/`echo $ARCH | sed s/i686-linux-android/x86/`-4.9/prebuilt/$SYSTEM/bin/$ARCH-strip $ARCHDIR/libsecp256k1.so
done

cd ..
