#!/usr/bin/env bash
set -e

ANDROID_SDK_ROOT=/opt/android-sdk-linux
COMMAND_LINE_TOOLS_FILENAME=commandlinetools-linux-9477386_latest.zip
COMMAND_LINE_TOOLS_HASH=bd1aa17c7ef10066949c88dc6c9c8d536be27f992a1f3b5a584f9bd2ba5646a0

apt update -qq
apt upgrade -yqq
apt install -yqq --no-install-recommends ca-certificates-java unzip curl gzip perl git sed software-properties-common gnupg openjdk-11-jdk openjdk-17-jdk

ARCH=$(uname -m)
if [[ $ARCH = "aarch64" || $ARCH = "arm" ]]; then
  update-java-alternatives -s java-1.17.0-openjdk-arm64
else
  update-java-alternatives -s java-1.17.0-openjdk-amd64
fi

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

${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager "tools" "platform-tools" "build-tools;30.0.3" "build-tools;33.0.2" "build-tools;33.0.1" "build-tools;34.0.0"
${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager "extras;android;m2repository" "extras;google;m2repository"

# The `yes` is for accepting all non-standard tool licenses.
yes | ${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager "platforms;android-34"

apt autoremove -yqq
apt clean -yqq
rm -rf /var/lib/apt/lists/* /var/cache/* /tmp/*