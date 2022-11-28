#!/bin/bash
set -e

# --- Help
help_message() {
  cat <<- _EOF_
  Generate data for adhoc distribution in dest folder

  Usage: $SCRIPT_NAME [-h|--help] -a|--app [-d|--dest] -u|--url

  Options:
    -a, --app Pass ipa app path
    -d, --dest The destination folder (default dist)
    -u, --url Base url to publish
    -h, --help  Display this help message and exit

_EOF_
  exit 0
}

# --- Argument handling
# https://stackoverflow.com/questions/192249/how-do-i-parse-command-line-arguments-in-bash
POSITIONAL=()
while [[ $# -gt 0 ]]
do
key="$1"
case $key in
    -h | --help)
      help_message ;;
    -a | --app)
      APP=${2}
      shift 2;;
    -d | --dest)
      DEST=${2}
      shift 2;;
    -u | --url)
      URL=${2}
      shift 2;;
    *)    # unknown option
    POSITIONAL+=("$1") # save it in an array for later
    shift # past argument
    ;;
esac
done

set -- "${POSITIONAL[@]:-}" # restore positional parameters

# --- Setup variables
LAST_TAG=$(git describe --abbrev=0 | sed -e "s/^release_//")
BASENAME_IPA=$(basename -- $APP)
NAME_IPA=${BASENAME_IPA%.ipa}
URL_IPA=${URL}/${BASENAME_IPA}
if [ -z ${DEST} ]; then
    DEST="./dist"
fi

# --- Build distribution files
mkdir -p ${DEST}
cp ${APP} ${DEST} | true

cat > "${DEST}/index.html" <<EOL
<html>
<body>
<h1>
<a href="itms-services://?action=download-manifest&url=${URL}/manifest.plist">Install App</a>
</h1>
</body>
</html>
EOL

cat > "${DEST}/manifest.plist" <<EOL
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>items</key>
    <array>
        <dict>
            <key>assets</key>
            <array>
                <dict>
                    <key>kind</key>
                    <string>software-package</string>
                    <key>url</key>
                    <string>${URL_IPA}</string>
                </dict>
            </array>
            <key>metadata</key>
            <dict>
                <key>bundle-identifier</key>
                <string>io.blockstream.greendev</string>
                <key>bundle-version</key>
                <string>${LAST_TAG}</string>
                <key>kind</key>
                <string>software</string>
                <key>title</key>
                <string>${NAME_IPA}</string>
            </dict>
        </dict>
    </array>
</dict>
</plist>
EOL
