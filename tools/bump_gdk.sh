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

TAGNAME=$(curl https://api.github.com/repos/blockstream/gdk/releases/latest | jq -r .tag_name)
NAME=$(curl https://api.github.com/repos/blockstream/gdk/releases/latest | jq -r .name)
	sed -i '' -e "s/TAGNAME=.*/TAGNAME=\"${TAGNAME}\"/" ./tools/fetch_gdk_binaries.sh
sed -i '' -e "s/TAGNAME=.*/TAGNAME=\"${TAGNAME}\"/" ./tools/build.sh

for PLAT in "gdk-iphone" "gdk-iphone-sim"; do
   TARURL=$(curl https://api.github.com/repos/blockstream/gdk/releases/latest | jq -r ".assets[].browser_download_url" | grep $PLAT.tar.gz)
   TEMP=$(mktemp)
   curl -sL -o $TEMP $TARURL
   SHA256=$(shasum -a256 $TEMP | awk '{print $1;}')
   echo $SHA256
   if [[ "$PLAT" == "gdk-iphone" ]]; then
      sed -i '' -e "s/SHA256_IPHONE=.*/SHA256_IPHONE=\"${SHA256}\"/" ./tools/fetch_gdk_binaries.sh
   else
      sed -i '' -e "s/SHA256_IPHONESIM=.*/SHA256_IPHONESIM=\"${SHA256}\"/" ./tools/fetch_gdk_binaries.sh
   fi
done

git add ./tools/build.sh ./tools/fetch_gdk_binaries.sh
git commit -m "Update GDK to ${NAME}" -S
