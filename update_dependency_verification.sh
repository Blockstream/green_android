#!/bin/bash
set -e

# ----- Help
help_message() {
  cat <<- _EOF_
  Update Gradle Dependency Verification checksums

  Usage: $SCRIPT_NAME [-h|--help] [-l|--local] [-a|--add] [-al]

  Options:
    -h, --help  Display this help message and exit
    -l, --local Run in local environment
    -a, --add   Add new checksums
    -b, --build Run build task to get more dependencies (takes longer)
    -al         Run in local environment, only adding checksums

_EOF_
  exit 0
}

# ----- Vars
GRADLE="docker"
OVERWRITE=true
TASK="help"

# --- Argument handling
# https://stackoverflow.com/questions/192249/how-do-i-parse-command-line-arguments-in-bash
POSITIONAL=()
while [[ $# -gt 0 ]]
do
key="$1"

case $key in
    -h | --help)
      help_message ;;
    -l | --local)
      GRADLE="local"
      shift ;;
    -b | --build)
      TASK="assembleDevelopmentDebug"
      shift ;;
    -a | --add | --only-add)
      OVERWRITE=false
      shift ;;
    -al)
      GRADLE="local"
      OVERWRITE=false
      shift ;;
    *)    # unknown option
    POSITIONAL+=("$1") # save it in an array for later
    shift # past argument
    ;;
esac
done

set -- "${POSITIONAL[@]:-}" # restore positional parameters

# --- Execution
if [[ $OVERWRITE = true ]]; then
  # remove all checksums
  sed -i '' -e '/<components>/,/<\/components>/d' gradle/verification-metadata.xml
fi

# Run locally or in Docker
if [[ $GRADLE == "local" ]]; then
    ./gradlew --write-verification-metadata sha256 "${TASK}"
else
  docker run --rm --name green_dependency_verification -v $PWD:/ga greenaddress/android@sha256:c00c98afd6682d9a8e18917e75d1cfca24fa3b9575a783a9abaec826d8b23964 /bin/sh -c "cd /ga && ./gradlew --write-verification-metadata sha256 ${TASK}"
fi
