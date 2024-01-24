#!/usr/bin/env bash
# Downloads and installs the pre-built gdk libraries
set -e

# ----- Help
help_message() {
  cat <<-_EOF_
  Downloads and install the pre-built GDK libraries

  Usage: $SCRIPT_NAME [-h|--help] [-c|--commit sha256]

  Options:
    -c, --commit Download the provided commit
    -h, --help  Display this help message and exit

_EOF_
  exit 0
}

# ----- Vars
ARM_NAME="gdk-iphone"
ARM_SIM_NAME="gdk-iphonesim-arm64"
X86_SIM_NAME="gdk-iphonesim-x86_64"

ARM_TARBALL="gdk-iphone.tar.gz"
ARM_SIM_TARBALL="gdk-iphone-sim.tar.gz"
X86_SIM_TARBALL="gdk-iphone-sim-x86_64.tar.gz"
# The version of gdk to fetch and its sha256 checksum for integrity checking
TAGNAME="release_0.70.0"
ARM_URL="https://github.com/Blockstream/gdk/releases/download/${TAGNAME}/${ARM_TARBALL}"
ARM_SIM_URL="https://github.com/Blockstream/gdk/releases/download/${TAGNAME}/${ARM_SIM_TARBALL}"
X86_SIM_URL="https://github.com/Blockstream/gdk/releases/download/${TAGNAME}/${X86_SIM_TARBALL}"
ARM_SHA256="15f4ab04f3f7fdfb6d11703938954b85dd82b2c4fb88f39b87028f5fcd505f41"
ARM_SIM_SHA256="318cc1f8c018c84b10e37dff109de220dc57f44d99ac0a97cf64cbdd2d372d9b"
X86_SIM_SHA256="17b3a77d00c7591484ee9f7eaff5603edead33a169d7ecbabe58d08df306933f"
VALIDATE_CHECKSUM=true
COMMIT=false
GCLOUD_URL="https://storage.googleapis.com/green-gdk-builds/gdk-"

# --- Argument handling
# https://stackoverflow.com/questions/192249/how-do-i-parse-command-line-arguments-in-bash
POSITIONAL=()
while [[ $# -gt 0 ]]; do
  key="$1"

  case $key in
  -h | --help)
    help_message
    ;;
  -c | --commit)
    COMMIT=${2}
    shift 2
    ;;
  *)                   # unknown option
    POSITIONAL+=("$1") # save it in an array for later
    shift              # past argument
    ;;
  esac
done

set -- "${POSITIONAL[@]:-}" # restore positional parameters

# Pre-requisites
function check_command() {
  command -v $1 >/dev/null 2>&1 || {
    echo >&2 "$1 not found, exiting."
    exit 1
  }
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
COMMON_MODULE_ROOT=${OLD_PWD}
if [ -d "${COMMON_MODULE_ROOT}/common" ]; then
  COMMON_MODULE_ROOT="${COMMON_MODULE_ROOT}/common"
fi

#JNI_LIBS_DIR=${GDK_MODULE_ROOT}/src/main/jniLibs
#GDK_JAVA_DIR="${GDK_MODULE_ROOT}/src/main/java/com/blockstream"

# Clean up any previous install
rm -rf $COMMON_MODULE_ROOT/src/include

mkdir -p $COMMON_MODULE_ROOT/src/include

download() {
  IS_SIM=$1
  NAME=$2
  TARBALL=$3
  URL=$4
  SHA256=$5
  PLATFORM=$6
  # Fetch, validate and decompress gdk
  echo "Downloading from $URL"
  curl -sL -o ${TARBALL} "${URL}"
  if [[ $VALIDATE_CHECKSUM = true ]]; then
    echo "Validating checksum $SHA256"
    echo "${SHA256} ${TARBALL}"
    shasum -a 256 ${TARBALL}
    echo "${SHA256}  ${TARBALL}" | shasum -a 256 --check
  fi

  tar xvf ${TARBALL}

  if [[ $IS_SIM = true ]]; then
    mkdir -p $COMMON_MODULE_ROOT/src/libs/ios_simulator_$PLATFORM
    cp $NAME/lib/iphonesimulator/libgreenaddress_full.a $COMMON_MODULE_ROOT/src/libs/ios_simulator_$PLATFORM
  else

    # Copy header files
    mkdir -p $COMMON_MODULE_ROOT/src/include
    cp $NAME/include/gdk/*/*.h $COMMON_MODULE_ROOT/src/include/
    cp $NAME/include/gdk/*.h $COMMON_MODULE_ROOT/src/include/
    cp $NAME/include/gdk/module.modulemap $COMMON_MODULE_ROOT/src/include/

    mkdir -p $COMMON_MODULE_ROOT/src/libs/ios_$PLATFORM
    cp $NAME/lib/iphoneos/libgreenaddress_full.a $COMMON_MODULE_ROOT/src/libs/ios_$PLATFORM
  fi

  # Cleanup
  rm ${TARBALL}
  rm -fr $NAME
}

download false $ARM_NAME $ARM_TARBALL $ARM_URL $ARM_SHA256 "arm64"
download true $ARM_SIM_NAME $ARM_SIM_TARBALL $ARM_SIM_URL $ARM_SIM_SHA256 "arm64"
download true $X86_SIM_NAME $X86_SIM_TARBALL $X86_SIM_URL $X86_SIM_SHA256 "x86"
