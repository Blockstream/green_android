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
    -c, --commit      Checkout commit
    -g, --gdk         Download GDK [app (app version), master (latest master), commitHash (specific commit hash)]
    -d, --development Development flavor
    -a, --add-wallet  Add wallet and login credentials
    -q, --qa          Launch QA Tester activity
    -u, --uninstall   Uninstall before install

_EOF_
  exit 0
}

# ----- Vars
FLAVOR="production"
GDK=false
ADD_WALLET=false
BRANCH=false
COMMIT=false
UNINSTALL=false
PACKAGE="com.greenaddress.greenbits_android_wallet"
LAUNCH_ACTIVITY="GreenActivity"

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
    -c | --commit)
      shift
      COMMIT=$1
      shift ;;
    -g | --gdk)
      shift
      GDK=$1
      shift ;;
    -a | --add-wallet)
      shift
      ADD_WALLET=$1
      shift ;;
    -u | --uninstall)
      UNINSTALL=true
      shift ;;
    -d | --development)
      FLAVOR="development"
      shift ;;
    -f | --fdroid)
      FLAVOR="fdroid"
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
  elif [[ $FLAVOR == "fdroid" ]]; then
    ./gradlew uninstallProductionFDroidDebug
  else
    ./gradlew uninstallDevelopmentDebug
  fi
}

install(){
  if [[ $FLAVOR == "production" ]]; then
    ./gradlew installProductionGoogleDebug
  elif [[ $FLAVOR == "fdroid" ]]; then
    ./gradlew installProductionFDroidDebug
  else
    ./gradlew installDevelopmentDebug
  fi
}

launch(){
  # kill
  adb shell am force-stop $PACKAGE
  # launch
  if [[ $1 != false ]]; then
    adb shell am start -n "$PACKAGE/com.blockstream.green.$LAUNCH_ACTIVITY" --es ADD_WALLET $1
  else
    adb shell am start -n "$PACKAGE/com.blockstream.green.$LAUNCH_ACTIVITY"
  fi

}

checkout(){
  echo "Checking out $1"
  git checkout $1
  if [[ $BRANCH != false ]]; then
    git reset --hard origin/$1
  else
    git reset --hard $1
  fi

  git status
}

gdk(){
  if [[ $GDK == "app" ]]; then
      ./gdk/fetch_android_binaries.sh
  else
    # Export the GDK_COMMIT for it to be appended in app version
    export GDK_COMMIT=$1
    ./gdk/fetch_android_binaries.sh -c $1
  fi
}

info(){

  echo -e "\n--------------------------------------------------------"
  echo "Flavor:         $FLAVOR"
  echo "Branch:         `git branch --show-current`"
  echo "Commit Hash:    `git rev-parse HEAD`"
  echo -e "--------------------------------------------------------\n"
}

# --- Execution
if [[ $FLAVOR == "development" ]]; then
    PACKAGE="$PACKAGE.dev"
fi

if [[ $BRANCH != false ]]; then
  checkout $BRANCH
fi

if [[ $COMMIT != false ]]; then
  checkout $COMMIT
fi

info

if [[ $UNINSTALL = true ]]; then
  uninstall
fi

if [[ $GDK != false ]]; then
  gdk $GDK
fi

install
launch $ADD_WALLET

info