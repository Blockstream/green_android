#!/usr/bin/env bash
set -e

ANDROID_SDK_ROOT=/opt/android-sdk-linux
COMMAND_LINE_TOOLS_FILENAME=commandlinetools-linux-7583922_latest.zip
COMMAND_LINE_TOOLS_HASH=124f2d5115eee365df6cf3228ffbca6fc3911d16f8025bebd5b1c6e2fcfa7faf

apt update -qq
apt upgrade -yqq
apt install -yqq --no-install-recommends ca-certificates-java unzip curl gzip perl git software-properties-common gnupg openjdk-11-jdk
update-java-alternatives -s java-1.11.0-openjdk-amd64


cd /opt && curl -sSO https://dl.google.com/android/repository/${COMMAND_LINE_TOOLS_FILENAME}
echo "${COMMAND_LINE_TOOLS_HASH} ${COMMAND_LINE_TOOLS_FILENAME}" | sha256sum --check
mkdir -p ${ANDROID_SDK_ROOT}/cmdline-tools
unzip -q ${COMMAND_LINE_TOOLS_FILENAME} -d /tmp/ && rm ${COMMAND_LINE_TOOLS_FILENAME}
mv /tmp/cmdline-tools/ ${ANDROID_SDK_ROOT}/cmdline-tools/latest
ls -la ${ANDROID_SDK_ROOT}/cmdline-tools/latest/

# Accept licenses before installing components, no need to echo y for each component
# License is valid for all the standard components in versions installed from this file
# Non-standard components: MIPS system images, preview versions, GDK (Google Glass) and Android Google TV require separate licenses, not accepted there
yes | ${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager --licenses

${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager "tools" "platform-tools" "build-tools;31.0.0"
${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager "extras;android;m2repository" "extras;google;m2repository"

# The `yes` is for accepting all non-standard tool licenses.
yes | ${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager "platforms;android-31"

apt autoremove -yqq
apt clean -yqq
rm -rf /var/lib/apt/lists/* /var/cache/* /tmp/*