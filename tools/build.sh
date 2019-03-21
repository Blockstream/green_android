#/usr/bin/env bash
set -e

GETOPT='/usr/local/opt/gnu-getopt/bin/getopt'

if (($# < 1)); then
    echo 'Usage: build.sh --iphone/--iphonesim.'
    exit 1
fi

TEMPOPT=`"$GETOPT" -n "build.sh" -o s,d -l iphone,iphonesim -- "$@"`
eval set -- "$TEMPOPT"
while true; do
    case $1 in
        --iphone) DEVICE=iphoneos; TARGET=iphone; shift ;;
        --iphonesim) DEVICE=iphonesim; TARGET=iphonesim; shift ;;
        -- ) break ;;
    esac
done

if [ ! -d gdk ]; then
    git clone https://github.com/Blockstream/gdk.git
fi

cd gdk
git fetch origin -t
git checkout release_0.0.8
rm -rf build-*
./tools/build.sh --$TARGET static
cd ..

if [ ! -d Pods ]; then
    pod install --repo-update
fi

SDK=$(xcodebuild -showsdks | grep $DEVICE | tr -s ' ' | tr -d '\-' | cut -f 3-)
xcodebuild CODE_SIGN_STYLE="Manual" PROVISIONING_PROFILE="b38d1a3f-9e58-491f-8e19-f9a9db0bbd45" DEVELOPMENT_TEAM="D9W37S9468" CODE_SIGN_IDENTITY="iPhone Distribution" -$SDK -workspace gaios.xcworkspace -scheme gaios clean archive -configuration release -archivePath ./build/Green.xcarchive
xcodebuild -exportArchive -archivePath ./build/Green.xcarchive -exportOptionsPlist ExportOptions.plist -exportPath ./build/Green.ipa
