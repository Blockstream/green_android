#!/usr/bin/env bash

# Upgrades gdk to latest available at github.com/Blockstream/gdk

function check_command() {
    command -v $1 >/dev/null 2>&1 || { echo >&2 "$1 not found, exiting."; exit 1; }
}

check_command awk
check_command mktemp
check_command grep
check_command sed
check_command curl
check_command jq
check_command shasum

# ----- Help
help_message() {
  cat <<- _EOF_
  Update GDK

  Usage: $SCRIPT_NAME [-h|--help] [-t|--tag]

  Options:
    -h, --help  Display this help message and exit
    -t, --t     Specify tag name

_EOF_
  exit 0
}

# ----- Vars
TAGNAME=false

# --- Argument handling
# https://stackoverflow.com/questions/192249/how-do-i-parse-command-line-arguments-in-bash
POSITIONAL=()
while [[ $# -gt 0 ]]
do
key="$1"

case $key in
    -h | --help)
      help_message ;;
    -t | --tag)
      TAGNAME=${2}
      shift 2;;
    *)    # unknown option
    POSITIONAL+=("$1") # save it in an array for later
    shift # past argument
    ;;
esac
done

set -- "${POSITIONAL[@]:-}" # restore positional parameters

# --- Execution
if [[ $TAGNAME = false ]]; then
  TAGNAME=$(curl https://api.github.com/repos/blockstream/gdk/releases/latest | jq -r .tag_name)
fi

NAME=$(curl https://api.github.com/repos/blockstream/gdk/releases/tags/$TAGNAME | jq -r .name)
sed -i '' -e "s/TAGNAME=.*/TAGNAME=\"${TAGNAME}\"/" ../crypto/fetch_gdk_binaries.sh
sed -i '' -e "s/TAGNAME=.*/TAGNAME=\"${TAGNAME}\"/" ../crypto/prepare_gdk_clang.sh

TEMP=$(mktemp)
TARURL=$(curl https://api.github.com/repos/blockstream/gdk/releases/tags/$TAGNAME | jq -r .assets[].browser_download_url | grep gdk-android-jni)

curl -sL -o $TEMP $TARURL
SHA256=$(shasum -a256 $TEMP | awk '{print $1;}')
echo $SHA256
sed -i '' -e "s/SHA256=.*/SHA256=\"${SHA256}\"/" ../crypto/fetch_gdk_binaries.sh


git add ../crypto/fetch_gdk_binaries.sh ../crypto/prepare_gdk_clang.sh
git commit -m "Update GDK to ${NAME}" -S
