#!/usr/bin/env bash
# Download and installs the pre-built wally libraries for use by GreenBits

set -e
URL="https://github.com/ElementsProject/libwally-core/releases/download/release_0.4.0/"
wget "${URL}/wallycore-android-jni.tar.gz"
echo "b90f9895a2e5a13bbadd4d458906a7f84c140d9905765b3873275b5aa58dd67a  wallycore-android-jni.tar.gz" | shasum -a 256 --check

gzip -d wallycore-android-jni.tar.gz
tar xf wallycore-android-jni.tar
rm -rf src/main/jniLibs/
mv wallycore-android-jni/lib/ src/main/jniLibs/
rm -r wallycore-android-jni*
