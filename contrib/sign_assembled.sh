#!/usr/bin/env bash
set -e
if [[ $# -eq 0 ]] ; then
    echo 'Usage: contrib/sign_assembled.sh $KEYSTORE $FILE_TO_SIGN1 $FILE_TO_SIGN2 $FILE_TO_SIGNN'
    exit 0
fi
rm -f tmp.apk
echo "Type the Keystore password for signer #1 followed by [ENTER]:"
read -s KS_PASS
KS=$1
shift
while (($#)); do
    RES=$(basename ${1%unsigned.apk*}signed.apk)
    if [ ! -f $RES ]; then
        echo "Processing $1"
        zipalign -v -p 4 $1 tmp.apk
        apksigner sign --v1-signing-enabled true --v2-signing-enabled true --ks $KS --ks-pass "pass:$KS_PASS" --out $RES tmp.apk
        apksigner verify $RES
        rm tmp.apk
    fi
    shift
done
