#!/usr/bin/env bash
set -e

# Packages such as tzdata otherwise stop the build to ask questions
export DEBIAN_FRONTEND=noninteractive

ANDROID_SDK_ROOT=/opt/android-sdk-linux
COMMAND_LINE_TOOLS_FILENAME=commandlinetools-linux-14742923_latest.zip
COMMAND_LINE_TOOLS_HASH=04453066b540409d975c676d781da1477479dde3761310f1a7eb92a1dfb15af7
# JetBrains Runtime that Gradle and every build run on (jvm in gradle/libs.versions.toml)
JBR_VERSION=25.0.5.0.3
JBR_BUILD=b700.17
JBR_HASH_X64=517ecc8fc4fc6937ec4b0c2063dcd56dff9b79c497b1b73ff3c2c2cd2f3cda8ea58e034dfaca8087389281c68cf2a5f5fff5f29d0a619c0715b52aa919dd5885
JBR_HASH_AARCH64=f5e306e008037f8118419bdcf1e0c70f42f549e946f285a9f592c7a59eefee0b0c0c4e5fc1a29534bcf727d72e42dd6eb622e7540479c1741daecfe0c941981f

apt update -qq
apt upgrade -yqq
apt install -yqq --no-install-recommends ca-certificates unzip curl gzip perl git sed software-properties-common gnupg

# The JetBrains Runtime goes to /usr/lib/jvm/jbr-25; the Dockerfile points JAVA_HOME and PATH at it
ARCH=$(uname -m)
if [[ $ARCH = "aarch64" ]]; then
  JBR_FILENAME=jbrsdk-${JBR_VERSION}-linux-aarch64-${JBR_BUILD}.tar.gz
  JBR_HASH=${JBR_HASH_AARCH64}
else
  JBR_FILENAME=jbrsdk-${JBR_VERSION}-linux-x64-${JBR_BUILD}.tar.gz
  JBR_HASH=${JBR_HASH_X64}
fi
cd /tmp && curl -sSLO https://cache-redirector.jetbrains.com/intellij-jbr/${JBR_FILENAME}
echo "${JBR_HASH} ${JBR_FILENAME}" | sha512sum --check
mkdir -p /usr/lib/jvm/jbr-25
tar -xzf ${JBR_FILENAME} -C /usr/lib/jvm/jbr-25 --strip-components=1 && rm ${JBR_FILENAME}
# sdkmanager below runs on it
export JAVA_HOME=/usr/lib/jvm/jbr-25
export PATH=${JAVA_HOME}/bin:${PATH}

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

${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager "platform-tools" "build-tools;36.0.0"
${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager "extras;android;m2repository" "extras;google;m2repository" "extras;google;google_play_services"

# The `yes` is for accepting all non-standard tool licenses.
yes | ${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager "platforms;android-37.0"

apt autoremove -yqq
apt clean -yqq
rm -rf /var/lib/apt/lists/* /var/cache/* /tmp/*