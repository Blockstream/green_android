#!/usr/bin/env bash

set -e

# ----- Help
help_message() {
  cat <<- _EOF_
  Run build jobs

  Usage: $SCRIPT_NAME  [-h|--help]

  Options:
    -h, --help        Display this help message and exit
    -g, --build-gdk   Build GDK from Source
    -u, --update      Update dependencies verification hashes
    -t, --task        Gradle Task

_EOF_
  exit 0
}

# ----- Vars
BUILD_GDK=false
UPDATE_DEPENDENCIES=false
GRADLE_TASK="assembleProductionRelease"

# --- Argument handling
# https://stackoverflow.com/questions/192249/how-do-i-parse-command-line-arguments-in-bash
POSITIONAL=()
while [[ $# -gt 0 ]]
do
key="$1"

case $key in
    -h | --help)
      help_message ;;
    -g | --build-gdk)
          BUILD_GDK=true
          shift ;;
    -u | --update)
          UPDATE_DEPENDENCIES=true
          shift ;;
    -t | --task)
          GRADLE_TASK=${2}
          shift 2;;
    *)    # unknown option
    POSITIONAL+=("$1") # save it in an array for later
    shift # past argument
    ;;
esac
done

set -- "${POSITIONAL[@]:-}" # restore positional parameters

## --- Execution

cd /ga

if [[ $UPDATE_DEPENDENCIES != false ]]; then
  printf "\nUpdate Dependencies Verification...\n\n"
  ./update_dependency_verification.sh "$@"
  exit 0
fi

cd gdk
if [[ $BUILD_GDK != false ]]; then
  printf "\nBuild GDK...\n\n"
  # Not tested, needs more deps
  ./prepare_gdk_clang.sh
fi

cd ..

printf "\nRun Gradle task: ${GRADLE_TASK}...\n\n"

./gradlew ${GRADLE_TASK}