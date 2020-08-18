#!/usr/bin/env bash
set -e

SDK_FILENAME=sdk-tools-linux-4333796.zip

apt-get -qq update
apt-get -yqq --no-install-recommends upgrade
apt-get -yqq --no-install-recommends install ca-certificates-java unzip curl gzip perl git software-properties-common gnupg

SHA256SUM_KEY=428ce45ffbc74e350d707d95c661de959a2e43129a869bd82d78d1556a936440
curl -sL -o public.key https://adoptopenjdk.jfrog.io/adoptopenjdk/api/gpg/key/public
echo "${SHA256SUM_KEY}  public.key" | sha256sum --check
apt-key add public.key
add-apt-repository --yes https://adoptopenjdk.jfrog.io/adoptopenjdk/deb/

apt update -qq
apt install --no-install-recommends -yqq adoptopenjdk-8-hotspot
update-java-alternatives -s adoptopenjdk-8-hotspot-amd64

ANDROID_SDK_HASH=92ffee5a1d98d856634e8b71132e8a95d96c83a63fde1099be3d86df3106def9

cd /opt && curl -sSO https://dl.google.com/android/repository/${SDK_FILENAME}
echo "${ANDROID_SDK_HASH} ${SDK_FILENAME}" | sha256sum --check
unzip -qq ${SDK_FILENAME} && rm ${SDK_FILENAME}


#FIXME: avoid installing emulator
yes | /opt/tools/bin/sdkmanager "tools" "platform-tools"
yes | /opt/tools/bin/sdkmanager "build-tools;29.0.2"
yes | /opt/tools/bin/sdkmanager "platforms;android-29"
yes | /opt/tools/bin/sdkmanager "extras;android;m2repository" "extras;google;m2repository"
apt-get -yqq autoremove && apt-get -yqq clean
rm -rf /var/lib/apt/lists/* /var/cache/* /tmp/*
