#!/bin/bash

set -e
if [ -z "$JAVA_HOME" ]; then
    export JAVA_HOME=$JAVA7_HOME;
fi

echo ${JAVA_HOME:?}
echo ${ANDROID_NDK:?}

cd scrypt
patch -p1 < ../scrypt_Makefile.patch

if [ `uname` == "Darwin" ]; then
    export SYSTEM="darwin-x86_64"
elif [ `uname -m` = "x86_64" ]; then
    export SYSTEM="linux-x86_64"
else
    export SYSTEM="linux-x86"
fi

for ARCH in aarch64-linux-android mips64el-linux-android x86_64 arm-linux-androideabi mipsel-linux-android x86; do
    export ARCH_SHORT=`echo $ARCH | cut -d'-' -f 1 | sed s/mipsel/mips/ | sed s/aarch64/arm64/ | sed s/mips64el/mips64/` # arm|mips|x86
    export PATH=$ANDROID_NDK/toolchains/$ARCH-4.9/prebuilt/$SYSTEM/bin:$PATH
    make clean TARGET=android
    make NDK_ROOT=$ANDROID_NDK TARGET=android ARCH_SHORT=$ARCH_SHORT ARCH=`echo $ARCH | sed s/x86$/i686-linux-android/ | sed s/x86_64/x86_64-linux-android/`
    ARCHDIR=../src/main/jniLibs/`echo $ARCH_SHORT | sed s/arm$/armeabi/ | sed s/arm64/arm64-v8a/` # armeabi|mips|x86
    mkdir -p $ARCHDIR
    cp target/libscrypt.so $ARCHDIR
    $ANDROID_NDK/toolchains/$ARCH-4.9/prebuilt/$SYSTEM/bin/`echo $ARCH | sed s/x86$/i686-linux-android/ | sed s/x86_64/x86_64-linux-android/`-strip $ARCHDIR/libscrypt.so
done
patch -p1 -R < ../scrypt_Makefile.patch

cd ../secp256k1

./autogen.sh

for ARCH in aarch64-linux-android mips64el-linux-android x86_64 arm-linux-androideabi mipsel-linux-android x86; do
    export PATH=$ANDROID_NDK/toolchains/$ARCH-4.9/prebuilt/$SYSTEM/bin:$PATH

    export ARCH_SHORT=`echo $ARCH | cut -d'-' -f 1 | sed s/mipsel/mips/ | sed s/aarch64/arm64/ | sed s/mips64el/mips64/` # arm|mips|x86
    export ARCH=`echo $ARCH | sed s/x86$/i686-linux-android/ | sed s/x86_64/x86_64-linux-android/`

    export SYSROOT=$ANDROID_NDK/platforms/android-23/arch-$ARCH_SHORT/
    export CC=$ARCH-gcc
    export CPP=$ARCH-cpp
    export CPPFLAGS=--sysroot=$SYSROOT
    export CFLAGS=--sysroot=$SYSROOT

    ./configure --host=$ARCH --target=$ARCH --enable-jni --enable-experimental --enable-module-schnorr --enable-module-ecdh
    make -o configure clean
    make -o configure
    ARCHDIR=../src/main/jniLibs/`echo $ARCH_SHORT | sed s/arm$/armeabi/ | sed s/arm64/arm64-v8a/` # armeabi|mips|x86
    mkdir -p $ARCHDIR
    cp .libs/libsecp256k1.so $ARCHDIR
    $ANDROID_NDK/toolchains/`echo $ARCH | sed s/i686-linux-android/x86/ | sed s/x86_64-linux-android/x86_64/`-4.9/prebuilt/$SYSTEM/bin/$ARCH-strip $ARCHDIR/libsecp256k1.so
done

cd ..
