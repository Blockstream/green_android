#!/bin/bash
if [ -f ".env" ]; then
    source ".env"
fi

for filename in configs/*.xcconfig; do
    sed -i '' -r "s/^[#]*\s*BREEZ_API_KEY =.*/BREEZ_API_KEY = ${BREEZ_API_KEY}/" $filename
    sed -i '' -r "s/^[#]*\s*GREENLIGHT_DEVICE_KEY =.*/GREENLIGHT_DEVICE_KEY = ${GREENLIGHT_DEVICE_KEY}/" $filename
    sed -i '' -r "s/^[#]*\s*GREENLIGHT_DEVICE_CERT =.*/GREENLIGHT_DEVICE_CERT = ${GREENLIGHT_DEVICE_CERT}/" $filename
    git update-index --assume-unchanged $filename
done
