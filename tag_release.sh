#!/bin/bash
set -e

# ----- Help
help_message() {
  cat <<- _EOF_
  Create a release tag and update assets

  Usage: $SCRIPT_NAME versionName [-h|--help]

  Options:
    -h, --help  Display this help message and exit

_EOF_
  exit 0
}

# ----- Vars
GRADLE_BUILD_FILE='green/build.gradle'

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

if [ -n "${1:-}" ]
then
  VERSION_NAME=${1}
else
  printf "You have to provide the VersionName eg. 1.3.5\n"
  exit 1
fi

# Pre-requisites
function check_command() {
    command -v $1 >/dev/null 2>&1 || { echo >&2 "$1 not found, exiting."; exit 1; }
}

# --- Check
check_command curl
check_command sed


# --- Execution

# Update Assets
cd crypto
./fetch_assets.sh
cd ..

printf "\nUpdating versionCode & VersionName...\n\n"

currentVersionCode=`awk '/ versionCode / {print $2}' $GRADLE_BUILD_FILE`
newVersionCode=$(($currentVersionCode + 1))

sed -i '' -e "s/versionCode .*/versionCode ${newVersionCode}/" $GRADLE_BUILD_FILE`
sed -i '' -e "s/versionName .*/versionName \"${VERSION_NAME}\"/" $GRADLE_BUILD_FILE`

printf "* versionCode: \t${newVersionCode}\n"
printf "* versionName: \t${VERSION_NAME}\n"

printf "\nCreating git commit...\n"
git add $GRADLE_BUILD_FILE
git commit -m "Increment to version ${VERSION_NAME}"