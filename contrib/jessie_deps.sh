#!/usr/bin/env bash
set -e

export SDK_FILENAME=sdk-tools-linux-3859397.zip
export NDK_FILENAME=android-ndk-r14b-linux-x86_64.zip

echo "deb http://http.debian.net/debian jessie-backports main" | tee -a /etc/apt/sources.list
mkdir -p ~/.gradle/
touch ~/.gradle/gradle.properties
echo "org.gradle.daemon=true" >> ~/.gradle/gradle.properties

dpkg --add-architecture i386
sed -i 's/deb.debian.org/httpredir.debian.org/g' /etc/apt/sources.list
apt-get -yqq update && apt-get -yqq upgrade
apt-get -yqq install -t jessie-backports openjdk-8-jdk ca-certificates-java
apt-get -yqq install unzip curl make swig autoconf libtool pkg-config libc6:i386 libc6-dev:i386 libncurses5:i386 libstdc++6:i386 lib32z1 python
update-java-alternatives -s java-1.8.0-openjdk-amd64

cd /opt && curl -sSO https://dl.google.com/android/repository/${SDK_FILENAME} && unzip -qq ${SDK_FILENAME} && rm ${SDK_FILENAME}
cd /opt && curl -sSO https://dl.google.com/android/repository/${NDK_FILENAME} && unzip -qq ${NDK_FILENAME} && rm ${NDK_FILENAME}

mkdir -p /opt/licenses
echo 8933bad161af4178b1185d1a37fbf41ea5269c55 > /opt/licenses/android-sdk-license
#FIXME: avoid installing emulator
/opt/tools/bin/sdkmanager "tools" "platform-tools"
/opt/tools/bin/sdkmanager "build-tools;25.0.3"
/opt/tools/bin/sdkmanager "platforms;android-25"
/opt/tools/bin/sdkmanager "extras;android;m2repository" "extras;google;m2repository"
apt-get remove --purge curl unzip -yqq
apt-get -yqq autoremove && apt-get -yqq clean
rm -rf /var/lib/apt/lists/* /var/cache/* /tmp/*
