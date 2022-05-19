#!/bin/bash
set -e

# ----- Help
help_message() {
  cat <<- _EOF_
  Helper script to build a branch run it on a device

  Usage: $SCRIPT_NAME [-b|--branch branch] [-g|--gdk app|master|commitHash] [-d|--development] [-u|--uninstall] [-h|--help]

  Options:
    -h, --help        Display this help message and exit
    -b, --branch      Checkout remote branch
    -g, --gdk         Download GDK [app (app version), master (latest master), commitHash (specific commit hash)]
    -d, --development Development flavor
    -q, --qa          Launch QA Tester activity
    -u, --uninstall   Uninstall before install

_EOF_
  exit 0
}

# ----- Vars
FLAVOR="production"
GDK=false
BRANCH=false
UNINSTALL=false
PACKAGE="com.greenaddress.greenbits_android_wallet"
LAUNCH_ACTIVITY="MainActivity"

# --- Argument handling
# https://stackoverflow.com/questions/192249/how-do-i-parse-command-line-arguments-in-bash
POSITIONAL=()
while [[ $# -gt 0 ]]
do
key="$1"

case $key in
    -h | --help)
      help_message ;;
    -b | --branch)
      shift
      BRANCH=$1
      shift ;;
    -g | --gdk)
      shift
      GDK=$1
      shift ;;
    -u | --uninstall)
      UNINSTALL=true
      shift ;;
    -d | --development)
      FLAVOR="development"
      shift ;;
    -b | --branch )
      shift
      BRANCH=("$1")
      shift ;;
    -q | --qa )
      LAUNCH_ACTIVITY="QATesterActivity"
      shift ;;
    *)    # unknown option
    POSITIONAL+=("$1") # save it in an array for later
    shift # past argument
    ;;
esac
done

set -- "${POSITIONAL[@]:-}" # restore positional parameters

# --- Functions
uninstall(){
  echo "Uninstalling.."
  if [[ $FLAVOR == "production" ]]; then
    ./gradlew uninstallProductionDebug
  else
    ./gradlew uninstallDevelopmentDebug
  fi
}

install(){
  if [[ $FLAVOR == "production" ]]; then
    ./gradlew installProductionDebug
  else
    ./gradlew installDevelopmentDebug
  fi
}

launch(){
  # kill
  adb shell am force-stop $PACKAGE
  # launch
  adb shell am start -n "$PACKAGE/com.blockstream.green.ui.$LAUNCH_ACTIVITY"
}

checkout(){
  echo "Checking out $1"
  git checkout $1
  git reset --hard origin/$1
  git status
}

gdk(){
  if [[ $GDK == "app" ]]; then
      ./crypto/fetch_gdk_binaries.sh
  else
    # Export the GDK_COMMIT for it to be appended in app version
    export GDK_COMMIT=$1
    ./crypto/fetch_gdk_binaries.sh -c $1
  fi
}

# --- Execution
if [[ $FLAVOR == "development" ]]; then
    PACKAGE="$PACKAGE.dev"
fi

if [[ $BRANCH != false ]]; then
  checkout $BRANCH
fi

if [[ $UNINSTALL = true ]]; then
  uninstall
fi

if [[ $GDK != false ]]; then
  gdk $GDK
fi

install
launch

echo "--------------------------------------------------------"
echo "Branch:         `git branch --show-current`"
echo "Commit Hash:    `git rev-parse HEAD`"
echo "--------------------------------------------------------"