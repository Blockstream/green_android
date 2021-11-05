#!/usr/bin/env bash
# Downloads and installs the pre-built gdk libraries for use by green_ios
set -e

# The version of gdk to fetch and its sha256 checksum for integrity checking
if [[ $1 == "--simulator" ]]; then
    # Get version for iphone simulator
    NAME="gdk-iphone-sim"
    SHA256_IPHONESIM="af789d4ef02e01d17dad95d4739ba4028a49c53c3bcdb2862ad18a63abb969e1"
    SHA256=${SHA256_IPHONESIM}
else
    NAME="gdk-iphone"
    SHA256_IPHONE="d433f353c3d5cd2bea13c7ec06acb0ce85299f93334b67cd241929d1a46c13c4"
    SHA256=${SHA256_IPHONE}
fi

# Setup gdk version and url
TAGNAME="release_0.0.47"
TARBALL="${NAME}.tar.gz"
URL="https://github.com/Blockstream/gdk/releases/download/${TAGNAME}/${TARBALL}"

# Pre-requisites
function check_command() {
    command -v $1 >/dev/null 2>&1 || { echo >&2 "$1 not found, exiting."; exit 1; }
}
check_command curl
check_command gzip
check_command shasum

# Find out where we are being run from to get paths right
if [ ! -d "$(pwd)/gaios" ]; then
    echo "Run fetch script from gaios project root folder"
    exit 1
fi

# Clean up any previous install
rm -rf gdk-iphone

# Fetch, validate and decompress gdk
curl -sL -o ${TARBALL} "${URL}"
echo "${SHA256}  ${TARBALL}" | shasum -a 256 --check
tar xf ${TARBALL}
rm ${TARBALL}
if [[ $1 == "--simulator" ]]; then
    mv -f ${NAME} "gdk-iphone"
fi

