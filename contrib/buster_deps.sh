#!/usr/bin/env bash
set -e

SDK_FILENAME=sdk-tools-linux-4333796.zip

apt update -qq
apt upgrade -yqq
apt install -yqq --no-install-recommends ca-certificates-java unzip curl gzip perl git software-properties-common gnupg openjdk-11-jdk
update-java-alternatives -s java-1.11.0-openjdk-amd64

ANDROID_SDK_HASH=92ffee5a1d98d856634e8b71132e8a95d96c83a63fde1099be3d86df3106def9

cd /opt && curl -sSO https://dl.google.com/android/repository/${SDK_FILENAME}
echo "${ANDROID_SDK_HASH} ${SDK_FILENAME}" | sha256sum --check
unzip -qq ${SDK_FILENAME} && rm ${SDK_FILENAME}


#FIXME: avoid installing emulator
yes | /opt/tools/bin/sdkmanager "tools" "platform-tools"
yes | /opt/tools/bin/sdkmanager "build-tools;29.0.2"
yes | /opt/tools/bin/sdkmanager "platforms;android-29"
yes | /opt/tools/bin/sdkmanager "extras;android;m2repository" "extras;google;m2repository"
apt autoremove -yqq
apt clean -yqq
rm -rf /var/lib/apt/lists/* /var/cache/* /tmp/*
