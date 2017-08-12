#!/usr/bin/env bash
# Downloads and installs the pre-built wally libraries for use by GreenBits
set -e

# The version of wally to fetch and its sha256 checksum for integrity checking
TARBALL="wallycore-android-jni.tar.gz"
URL="https://github.com/ElementsProject/libwally-core/releases/download/release_0.4.0/${TARBALL}"
SHA256="b90f9895a2e5a13bbadd4d458906a7f84c140d9905765b3873275b5aa58dd67a"

# Pre-requisites
function check_command() {
    command -v $1 >/dev/null 2>&1 || { echo >&2 "$1 not found, exiting."; exit 1; }
}
check_command wget
check_command gzip
check_command shasum
check_command javac
check_command jar

# Find out where we are being run from to get paths right
OLD_PWD=$(pwd)
APP_ROOT=${OLD_PWD}
if [ -d "${APP_ROOT}/app" ]; then
    APP_ROOT="${APP_ROOT}/app"
fi
WALLY_JAVA_DIR="${APP_ROOT}/libwally-core/src/swig_java/src/com/blockstream/libwally"

# Clean up any previous install
rm -rf wallycore-android-jni* ${APP_ROOT}/src/main/jniLibs ${WALLY_JAVA_DIR}

# Fetch, validate and decompress wally
wget -q "${URL}"
echo "${SHA256}  ${TARBALL}" | shasum -a 256 --check
gzip -d wallycore-android-jni.tar.gz && tar xf wallycore-android-jni.tar

# Move the libraries and Java wrapper where we need them
mv wallycore-android-jni/lib/ ${APP_ROOT}/src/main/jniLibs
mkdir -p ${WALLY_JAVA_DIR}
mv wallycore-android-jni/src/swig_java/src/com/blockstream/libwally/Wally.java ${WALLY_JAVA_DIR}

# Create the wally jar file for building against
cd ${APP_ROOT}/libwally-core/src/swig_java/src
javac -source 1.7 -target 1.7 com/blockstream/libwally/Wally.java
jar cf ../wallycore.jar com/blockstream/libwally/Wally*class

# Cleanup
rm -r ${OLD_PWD}/wallycore-android-jni* ${WALLY_JAVA_DIR}
