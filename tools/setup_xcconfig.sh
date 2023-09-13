#!/bin/bash
if [ -f ".env" ]; then
    source ".env"
fi

mkdir -p certs
echo "${GREENLIGHT_DEVICE_CERT}" > certs/green.crt
echo "${GREENLIGHT_DEVICE_KEY}" > certs/green.pem

for filename in configs/*.xcconfig; do
    sed -i '' -r "s/^[#]*\s*BREEZ_API_KEY =.*/BREEZ_API_KEY = ${BREEZ_API_KEY}/" $filename
    sed -i '' -r "s/^[#]*\s*ZENDESK_CLIENT_ID =.*/ZENDESK_CLIENT_ID = ${ZENDESK_CLIENT_ID}/" $filename
    git update-index --assume-unchanged $filename
done
