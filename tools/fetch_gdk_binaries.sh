#!/usr/bin/env bash
# Downloads and installs the pre-built gdk libraries for use by green_ios
set -e

# The version of gdk to fetch and its sha256 checksum for integrity checking
NAME="gdk-iphone"
TARBALL="${NAME}.tar.gz"
URL="https://github.com/Blockstream/gdk/releases/download/release_0.0.37/${TARBALL}"
SHA256="bdba13f3faba9f1fe2c4d5be889a89bdac4c12f76fbe1390894ce72e358dff89"
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

