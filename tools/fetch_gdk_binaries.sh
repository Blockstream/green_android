#!/usr/bin/env bash
# Downloads and installs the pre-built gdk libraries for use by green_ios
set -e

# The version of gdk to fetch and its sha256 checksum for integrity checking
if [[ $1 == "--simulator" ]]; then
    # Get version for iphone simulator
    NAME="gdk-iphone-sim"
    SHA256_IPHONESIM="0ccaecf7c292547d051397bf2b5027ed36efca587721e46ed588b7b05e6d584c"
    SHA256=${SHA256_IPHONESIM}
else
    NAME="gdk-iphone"
    SHA256_IPHONE="66306a7fe73838b88925066303e65af9b1ca2bada6db90789a153d83bbd62cba"
    SHA256=${SHA256_IPHONE}
fi

# Setup gdk version and url
TAGNAME="release_0.0.52"
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

