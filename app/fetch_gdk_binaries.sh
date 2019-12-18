#!/usr/bin/env bash
# Downloads and installs the pre-built gdk libraries for use by Green-Android
set -e

if [ -d gdk ]; then
    echo "Found a 'gdk' folder, exiting now"
    exit 0
fi

# The version of gdk to fetch and its sha256 checksum for integrity checking
NAME="gdk-android-jni"
TARBALL="${NAME}.tar.gz"
URL="https://github.com/Blockstream/gdk/releases/download/release_0.0.25/${TARBALL}"
SHA256="10039a10084d78749b7d8a3948af83d3662163dcfb6ba618d5b9fb17ca340853"
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

JNILIBSDIR=${APP_ROOT}/src/main/jniLibs
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

