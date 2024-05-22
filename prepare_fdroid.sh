#!/bin/bash

echo "Changing Breez SDK dependency from KMP to Android"
sed -i -e 's/api(libs.breez.sdk.kmp)/\/\/ api(libs.breez.sdk.kmp)/g' common/build.gradle.kts
sed -i -e 's/\/\/ api(libs.breez.sdk.android/api(libs.breez.sdk.android/g' common/build.gradle.kts
sed -i -e 's/\/\/ implementation("${libs.jna/implementation("${libs.jna/g' common/build.gradle.kts
