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
TAGNAME="release_0.0.44rc1"
URL="https://github.com/Blockstream/gdk/releases/download/${TAGNAME}/${TARBALL}"
SHA256="732305f5e425a52b88361438669453f5a68724ab96109b0dff4645572e8e9823"
# Pre-requisites
function check_command() {
    command -v $1 >/dev/null 2>&1 || { echo >&2 "$1 not found, exiting."; exit 1; }
}
check_command curl
check_command gzip
check_command shasum

# Find out where we are being run from to get paths right
OLD_PWD=$(pwd)
CRYPTO_MODULE_ROOT=${OLD_PWD}
if [ -d "${CRYPTO_MODULE_ROOT}/crypto" ]; then
    CRYPTO_MODULE_ROOT="${CRYPTO_MODULE_ROOT}/crypto"
fi

JNILIBSDIR=${CRYPTO_MODULE_ROOT}/src/main/jniLibs
GDK_JAVA_DIR="${CRYPTO_MODULE_ROOT}/src/main/java/com/blockstream"

# Clean up any previous install
rm -rf gdk-android-jni* ${CRYPTO_MODULE_ROOT}/src/main/jniLibs \
  ${GDK_JAVA_DIR}/libgreenaddress/GDK.java \
  ${GDK_JAVA_DIR}/libwally/Wally.java

# Fetch, validate and decompress gdk
curl -sL -o ${TARBALL} "${URL}"
echo "${SHA256}  ${TARBALL}" | shasum -a 256 --check
tar xvf ${TARBALL}
rm ${TARBALL}

# Move the libraries and Java wrapper where we need them
mv ${NAME}/lib/ ${CRYPTO_MODULE_ROOT}/src/main/jniLibs/
mv ${NAME}/java/com/blockstream/libgreenaddress/GDK.java ${GDK_JAVA_DIR}/libgreenaddress/GDK.java
mv ${NAME}/java/com/blockstream/libwally/Wally.java ${GDK_JAVA_DIR}/libwally/Wally.java

# Cleanup
rm -fr $NAME

