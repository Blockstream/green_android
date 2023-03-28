#!/bin/bash

set -e
if [ -z "$JAVA_HOME" ]; then
    export JAVA_HOME=$JAVA7_HOME
fi
echo ${JAVA_HOME:?}
echo ${ANDROID_NDK:?}

TAGNAME="release_0.0.58"

if [ -d gdk ]; then
    cd gdk
else
    git clone https://github.com/Blockstream/gdk.git
    cd gdk
    git checkout tags/${TAGNAME} -b ${TAGNAME}
fi

python3 -m virtualenv -p python3 venv

source venv/bin/activate

pip install -r tools/requirements.txt

all_archs="armeabi-v7a arm64-v8a x86_64 x86"

if [ -n "$1" ]; then
    all_archs="$1"
fi

mkdir -p ../src/main/java/com/blockstream/libwally ../src/main/java/com/blockstream/libgreenaddress/

for arch in $all_archs; do
    ./tools/builddeps.sh -ndk $arch --prefix $PWD/prebuild-$arch
    cmake -B build-android-$arch -S . -DEXTERNAL-DEPS-DIR:PATH=$PWD/prebuild-$arch -DCMAKE_TOOLCHAIN_FILE=cmake/profiles/android-$arch.cmake
    cmake --build build-android-$arch --target java-bindings --parallel 8
    cmake --install build-android-$arch --prefix $pwd/gdk-android-jni$arch --strip
    cmake --install build-android-$arch --prefix $pwd/gdk-android-jni$arch --component gdk-java
  
    cp gdk-android-jni$arch/lib/$arch/* ../src/main/jniLibs/$arch
    cp gdk-android-jni$arch/java/com/blockstream/libgreenaddress/GDK.java ../src/main/java/com/blockstream/libgreenaddress/GDK.java
    cp gdk-android-jni$arch/java/com/blockstream/libwally/Wally.java ../src/main/java/com/blockstream/libwally/Wally.java
done

cd ..

deactivate
