#!/bin/bash
#set -e

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
URL_ASSETS="https://assets.blockstream.info"
URL_ICONS="https://assets.blockstream.info/icons"

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
printf "Fetching asset JSON...\n"
curl -sL $URL_ASSETS > "${FILE_ASSETS_JSON}"
printf "Commiting ${FILE_ASSETS_JSON}\n\n"
git add "${FILE_ASSETS_JSON}"

# Fetch icons json
printf "Fetching icons JSON...\n"
ICONS_JSON=$(curl -sL $URL_ICONS)

jq -c -r 'keys | .[]' <<< "$ICONS_JSON" | while read id; do
    ICON_FILE="${ICONS_DIR}/asset_$id.png"
    printf "Saving %s to %s \n" "$id" "$ICON_FILE"
    ICON_BASE64=$(jq -c -r ".\"${id}\"" <<< "$ICONS_JSON")
    base64 -d <<< "$ICON_BASE64" > "${ICON_FILE}"
    git add "${ICON_FILE}"
done

printf "\nAssets updated\n"