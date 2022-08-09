#!/usr/bin/env bash
# Downloads and installs the pre-built gdk libraries for use by Green-Android
set -e

# ----- Help
help_message() {
  cat <<- _EOF_
  Downloads and install the pre-built GDK libraries

  Usage: $SCRIPT_NAME [-h|--help] [-c|--commit sha256]

  Options:
    -c, --commit Download the provided commit
    -h, --help  Display this help message and exit

_EOF_
  exit 0
}

# ----- Vars
NAME="gdk-android-jni"
TARBALL="${NAME}.tar.gz"
# The version of gdk to fetch and its sha256 checksum for integrity checking
TAGNAME="release_0.0.56"
URL="https://github.com/Blockstream/gdk/releases/download/${TAGNAME}/${TARBALL}"
SHA256="1867f106dcfe37b14095ccaa2322a61075b95f4c5367116b978d227de413ff10"
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

if [ -d gdk ]; then
    echo "Found a 'gdk' folder, exiting now"
    exit 0
fi


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

# Remove gdk_commit file if exists
if [ -f gdk_commit ] ; then
    rm gdk_commit
fi

if [[ $COMMIT != false ]]; then
  URL="${GCLOUD_URL}${COMMIT}/${TARBALL}"
  VALIDATE_CHECKSUM=false
  echo $COMMIT > gdk_commit
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

# Move the libraries and Java wrapper where we need them
mv ${NAME}/lib/ ${CRYPTO_MODULE_ROOT}/src/main/jniLibs/
mv ${NAME}/java/com/blockstream/libgreenaddress/GDK.java ${GDK_JAVA_DIR}/libgreenaddress/GDK.java
mv ${NAME}/java/com/blockstream/libwally/Wally.java ${GDK_JAVA_DIR}/libwally/Wally.java

# Cleanup
rm -fr $NAME
