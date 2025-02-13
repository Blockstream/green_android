#!/bin/bash
set -e

# ----- Help
help_message() {
  cat <<- _EOF_
  Update Gradle Dependency Verification checksums

  Usage: $SCRIPT_NAME [-h|--help] [-d|--docker] [-a|--add] [-al]

  Options:
    -h, --help        Display this help message and exit
    -d, --docker      Run in Docker container
    -o, --overwrite   Add new checksums
    -i, --ios         Build iOS
    -a, --assemble    Run assemble task to get more dependencies (takes longer)
    -b, --build       Run build task to get more dependencies (takes longer)
    -t, --test        Run test task to get more dependencies (takes longer)

_EOF_
  exit 0
}

# ----- Vars
GRADLE="local"
OVERWRITE=false
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
    -d | --docker)
      GRADLE="docker"
      shift ;;
    -b | --build)
      TASK="assembleDevelopmentDebug"
      shift ;;
    -i | --ios)
      TASK=":compose:assembleXCFramework"
      shift ;;
    --desktop)
      TASK="desktopMainClasses"
      shift ;;
    -a | --assemble)
      TASK="assemble"
      shift ;;
    -t | --test)
      TASK="test"
      shift ;;
    -o | --overwrite)
      OVERWRITE=true
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
  # TODO: Fix sed in linux
  sed -i '' -e '/<components>/,/<\/components>/d' gradle/verification-metadata.xml
fi

# Run locally or in Docker
if [[ $GRADLE == "local" ]]; then
    ./gradlew --write-verification-metadata sha256 "${TASK}"
else
  echo "Using Docker"
  # Deprecated: Use `docker run -it -v $PWD:/ga greenaddress/android -u` instead
  # docker run --rm -v $PWD:/ga --entrypoint /bin/sh greenaddress/android@sha256:de85c05b5ac837918a349e17e5085f57a59d5352a4f2f9029ab6174be8966429 "-c" "cd /ga && ./gradlew --write-verification-metadata sha256 ${TASK}"
  docker run --rm -it -v $PWD:/ga greenaddress/android -u --write-verification-metadata sha256 ${TASK}
fi
