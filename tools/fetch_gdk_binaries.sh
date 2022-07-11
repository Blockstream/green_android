#!/usr/bin/env bash
# Downloads and installs the pre-built gdk libraries for use by green_ios
set -e

# ----- Help
help_message() {
  cat <<- _EOF_
  Downloads and install the pre-built GDK libraries

  Usage: $SCRIPT_NAME [-h|--help] [-c|--commit sha256] [-s|--simulator]

  Options:
    -c, --commit Download the provided commit
    -s, --simulator Select iphone simulator platform
    -h, --help  Display this help message and exit

_EOF_
  exit 0
}

# ----- Vars
NAME="gdk-iphone"
SHA256="5a61a01dab7d97acd9116b4af130cf0ddc2c542bef630d4944091c622a814b5f"
TAGNAME="release_0.0.55"
TARBALL="${NAME}.tar.gz"
URL="https://github.com/Blockstream/gdk/releases/download/${TAGNAME}/${TARBALL}"
NAME_IPHONESIM="gdk-iphone-sim"
SHA256_IPHONESIM="67fc18ea334df575cb02c4500c1c294224577321710a961c4feeef87b2dc46ca"
SIMULATOR=false
VALIDATE_CHECKSUM=true
COMMIT=false
GCLOUD_URL="https://storage.googleapis.com/green-gdk-builds/gdk-"

# --- Argument handling
# https://stackoverflow.com/questions/192249/how-do-i-parse-command-line-arguments-in-bash
POSITIONAL=()
while [[ $# -gt 0 ]]
do
key="$1"

case $key in
    -h | --help)
      help_message ;;
    -c | --commit)
      COMMIT=${2}
      shift 2;;
    -s | --simulator)
      SIMULATOR=true
      shift 1;;
    *)    # unknown option
    POSITIONAL+=("$1") # save it in an array for later
    shift # past argument
    ;;
esac
done
set -- "${POSITIONAL[@]:-}" # restore positional parameters

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

if [[ $SIMULATOR == true ]]; then
    NAME=${NAME_IPHONESIM}
    SHA256=${SHA256_IPHONESIM}
    TARBALL="${NAME}.tar.gz"
    URL="https://github.com/Blockstream/gdk/releases/download/${TAGNAME}/${TARBALL}"
fi

if [[ $COMMIT != false ]]; then
  URL="${GCLOUD_URL}${COMMIT}/${TARBALL}"
  VALIDATE_CHECKSUM=false
fi

# Fetch, validate and decompress gdk
echo "Downloading from $URL"
curl -sL -o ${TARBALL} "${URL}"
if [[ $VALIDATE_CHECKSUM = true ]]; then
  echo "Validating checksum $SHA256"
  echo "${SHA256}  ${TARBALL}" | shasum -a 256 --check
fi

tar xvf ${TARBALL}
rm ${TARBALL}

if [[ $SIMULATOR == true ]]; then
    mv -f ${NAME} "gdk-iphone"
fi

