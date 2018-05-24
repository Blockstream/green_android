#!/usr/bin/env bash
set -e

export SDK_FILENAME=sdk-tools-linux-3859397.zip

dpkg --add-architecture i386
sed -i 's/deb.debian.org/httpredir.debian.org/g' /etc/apt/sources.list
apt-get -yqq update && apt-get -yqq upgrade
apt-get -yqq install openjdk-8-jdk ca-certificates-java unzip curl gzip perl
update-java-alternatives -s java-1.8.0-openjdk-amd64

cd /opt && curl -sSO https://dl.google.com/android/repository/${SDK_FILENAME} && unzip -qq ${SDK_FILENAME} && rm ${SDK_FILENAME}

#FIXME: avoid installing emulator
yes | /opt/tools/bin/sdkmanager "tools" "platform-tools"
yes | /opt/tools/bin/sdkmanager "build-tools;26.0.2"
yes | /opt/tools/bin/sdkmanager "platforms;android-27"
yes | /opt/tools/bin/sdkmanager "extras;android;m2repository" "extras;google;m2repository"
apt-get -yqq autoremove && apt-get -yqq clean
rm -rf /var/lib/apt/lists/* /var/cache/* /tmp/*
