#!/bin/bash
#set -e

# DISABLED
exit 0

# ----- Help
help_message() {
  cat <<- _EOF_
  Update assets cache file and icons

  Usage: $SCRIPT_NAME [-h|--help]

  Options:
    -h, --help  Display this help message and exit

_EOF_
  exit 0
}

# ----- Vars
SHA256="e12744dab766dd643327187f69bb50c3c5be49ef"

URL_ASSETS="https://raw.githubusercontent.com/Blockstream/asset_registry_db/${SHA256}/index.json"
URL_ICONS="https://raw.githubusercontent.com/Blockstream/asset_registry_db/${SHA256}/icons.json"

RES_DIR="src/main/res"
RAW_DIR="${RES_DIR}/raw"
FILE_ASSETS_JSON="${RAW_DIR}/assets.json"
ICONS_DIR="${RES_DIR}/drawable"


# --- Argument handling
# https://stackoverflow.com/questions/192249/how-do-i-parse-command-line-arguments-in-bash
POSITIONAL=()
while [[ $# -gt 0 ]]
do
key="$1"

case $key in
    -h | --help)
      help_message ;;
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


# --- Check
check_command curl
check_command jq
check_command base64


# --- Execution

# Save assets.json
printf "Fetching asset JSON (commit: ${SHA256})...\n"
curl -sL $URL_ASSETS --create-dirs -o "${FILE_ASSETS_JSON}"

# Fetch icons json
printf "Fetching icons JSON (commit: ${SHA256})...\n"
ICONS_JSON=$(curl -sL $URL_ICONS)

jq -c -r 'keys | .[]' <<< "$ICONS_JSON" | while read id; do
    ICON_FILE="${ICONS_DIR}/asset_$id.png"
    printf "Saving %s to %s \n" "$id" "$ICON_FILE"
    ICON_BASE64=$(jq -c -r ".\"${id}\"" <<< "$ICONS_JSON")
    base64 -d <<< "$ICON_BASE64" > "${ICON_FILE}"
done

printf "\nAssets updated\n"