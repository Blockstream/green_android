#!/bin/bash

set -euo pipefail

[ -z $ANDROID_HOME ]  && echo "ANDROID_HOME is not set" && exit -1

$ANDROID_HOME/platform-tools/adb --version
export AVD_PATH=$ANDROID_HOME/tools/bin

ls $AVD_PATH

if [[ $($AVD_PATH/avdmanager list avd  | sed -n '2p' | grep -c "Name") -lt 1 ]]; then
    echo "no virtual devices available"
    # create a new virtual device and set name
    # Download image and create virtual device
    $AVD_PATH/sdkmanager "system-images;android-25;google_apis;x86"
    $AVD_PATH/sdkmanager "emulator"
    echo no | $AVD_PATH/avdmanager --verbose create avd -n test-device -k "system-images;android-25;google_apis;x86"
    $ANDROID_HOME/emulator/emulator -avd test-device -verbose -no-window -no-audio -no-accel -gpu off
else
    export DEVICE_NAME=$($AVD_PATH/avdmanager list avd | grep "Name" | sed s/"^.*\:\ "//)
    echo "device name is $DEVICE_NAME"
fi

