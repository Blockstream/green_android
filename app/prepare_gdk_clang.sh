#!/bin/bash

set -e
if [ -z "$JAVA_HOME" ]; then
    export JAVA_HOME=$JAVA7_HOME
fi
echo ${JAVA_HOME:?}
echo ${ANDROID_NDK:?}

if [ -d gdk ]; then
    cd gdk
else
    git clone https://github.com/Blockstream/gdk.git
    cd gdk
    git checkout tags/release_0.0.24 -b release_0.0.24
fi

python3 -m virtualenv -p python3 venv

source venv/bin/activate

pip install -r tools/requirements.txt
pip install ninja

all_archs="armeabi-v7a arm64-v8a x86_64 x86"

if [ -n "$1" ]; then
    all_archs="$1"
fi

mkdir -p ../src/main/java/com/blockstream/libwally ../src/main/java/com/blockstream/libgreenaddress/

for arch in $all_archs; do
    mkdir -p ../src/main/jniLibs/$arch $PWD/gdk-android-jni$arch
    #./tools/build.sh --buildtype=debug --lto=false --install $PWD/gdk-android-jni$arch --ndk $arch
    ./tools/build.sh --install $PWD/gdk-android-jni$arch --ndk $arch --lto=true
    cp gdk-android-jni$arch/lib/$arch/* ../src/main/jniLibs/$arch
    cp gdk-android-jni$arch/java/com/blockstream/libgreenaddress/GDK.java ../src/main/java/com/blockstream/libgreenaddress/GDK.java
    cp gdk-android-jni$arch/java/com/blockstream/libwally/Wally.java ../src/main/java/com/blockstream/libwally/Wally.java
done

cd ..

deactivate
