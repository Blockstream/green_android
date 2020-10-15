#!/bin/bash

set -e

[ -z $ANDROID_HOME ]  && echo "ANDROID_HOME is not set" && exit -1

# Turn off animations
$ANDROID_HOME/platform-tools/adb shell settings put global window_animation_scale 0 &
$ANDROID_HOME/platform-tools/adb shell settings put global transition_animation_scale 0 &
$ANDROID_HOME/platform-tools/adb shell settings put global animator_duration_scale 0 &

echo "Running ui tests"
./gradlew connectedCheck --stacktrace
#./gradlew connectedProductionDebugAndroidTest
