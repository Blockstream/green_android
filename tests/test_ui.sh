#!/bin/bash

function wait_emulator () {
  boot_completed=false
  while [ "$boot_completed" == false ]; do
    status=$($ANDROID_HOME/platform-tools/adb wait-for-device shell getprop sys.boot_completed | tr -d '\r')
    echo "Boot Status: $status"

    if [ "$status" == "1" ]; then
      boot_completed=true
    else
      sleep 1
  fi
  done
}

set -e

[ -z $ANDROID_HOME ]  && echo "ANDROID_HOME is not set" && exit -1

wait_emulator

# Turn off animations
$ANDROID_HOME/platform-tools/adb shell settings put global window_animation_scale 0 &
$ANDROID_HOME/platform-tools/adb shell settings put global transition_animation_scale 0 &
$ANDROID_HOME/platform-tools/adb shell settings put global animator_duration_scale 0 &

echo "Running ui tests"
./gradlew connectedCheck --stacktrace
