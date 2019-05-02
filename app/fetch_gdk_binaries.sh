#!/usr/bin/env bash
# Downloads and installs the pre-built gdk libraries for use by Green-Android
set -e

# The version of gdk to fetch and its sha256 checksum for integrity checking
NAME="gdk-android-jni"
TARBALL="${NAME}.tar.gz"
URL="https://github.com/Blockstream/gdk/releases/download/release_0.0.11/${TARBALL}"
SHA256="f2e762259bcd0c7332c2d7e9d95a556f3fde09b6f87a4f82c20f9e1650c0f20a"

# Pre-requisites
function check_command() {
    command -v $1 >/dev/null 2>&1 || { echo >&2 "$1 not found, exiting."; exit 1; }
}
check_command curl
check_command gzip
check_command shasum

# Find out where we are being run from to get paths right
OLD_PWD=$(pwd)
APP_ROOT=${OLD_PWD}
if [ -d "${APP_ROOT}/app" ]; then
    APP_ROOT="${APP_ROOT}/app"
fi
GDK_JAVA_DIR="${APP_ROOT}/src/main/java/com/blockstream"

# Clean up any previous install
rm -rf gdk-android-jni* ${APP_ROOT}/src/main/jniLibs ${GDK_JAVA_DIR}

# Fetch, validate and decompress gdk
curl -sL -o ${TARBALL} "${URL}"
echo "${SHA256}  ${TARBALL}" | shasum -a 256 --check
tar xvf ${TARBALL}
rm ${TARBALL}

# Move the libraries and Java wrapper where we need them
mv ${NAME}/lib/ ${APP_ROOT}/src/main/jniLibs
mv ${NAME}/java/com/blockstream/ ${GDK_JAVA_DIR}

# Cleanup
rm -fr $NAME
