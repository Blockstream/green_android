#/usr/bin/env bash
set -e

GETOPT='/usr/local/opt/gnu-getopt/bin/getopt'

if (($# < 1)); then
    echo 'Usage: build.sh --iphone/--iphonesim.'
    exit 1
fi

SIGN_EXPORT=0

TEMPOPT=`"$GETOPT" -n "build.sh" -o s,d -l iphone,iphonesim,build-gdk,sign-and-export,update-cocoapods -- "$@"`
eval set -- "$TEMPOPT"
while true; do
    case $1 in
        --iphone) DEVICE=iphoneos; TARGET=iphone; shift ;;
        --iphonesim) DEVICE=iphonesim; TARGET=iphonesim; shift ;;
        --build-gdk) BUILD_GDK=1; shift ;;
        --sign-and-export) SIGN_EXPORT=1; shift ;;
        --update-cocoapods) UPDATE_COCOAPODS=1; shift ;;
        -- ) break ;;
    esac
done

export GEM_HOME=$HOME/.gem
export PATH=$GEM_HOME/bin:$PATH

if [[ "$UPDATE_COCOAPODS" -eq 1 ]]; then
    gem install cocoapods --user-install
fi

if [ ! -d Pods ]; then
    $(which pod) install
fi

# Call linter
Pods/SwiftLint/swiftlint --strict

# check gdk build
if [[ "$BUILD_GDK" -eq 1 ]]; then
    git clone https://github.com/Blockstream/gdk.git
    cd gdk
    git fetch origin -t
    git checkout release_0.0.38
    rm -rf build-*
    ./tools/build.sh --$TARGET static --lto=true --install=$PWD/../gdk-iphone
    cd ..
else
    ./tools/fetch_gdk_binaries.sh
fi

SDK=$(xcodebuild -showsdks | grep $DEVICE | tr -s ' ' | tr -d '\-' | cut -f 3-)
if [[ "$SIGN_EXPORT" -eq 1 ]]; then
    xcodebuild CODE_SIGN_STYLE="Manual" PROVISIONING_PROFILE="e79dac47-b4e7-4240-abab-12eb0e6fe96e" DEVELOPMENT_TEAM="D9W37S9468" CODE_SIGN_IDENTITY="iPhone Distribution" -$SDK -workspace gaios.xcworkspace -scheme gaios clean archive -configuration release -archivePath ./build/Green.xcarchive
    xcodebuild -exportArchive -archivePath ./build/Green.xcarchive -exportOptionsPlist ExportOptions.plist -exportPath ./build/Green.ipa
else
    xcodebuild CODE_SIGNING_ALLOWED=NO CODE_SIGN_IDENTITY="" CODE_SIGNING_REQUIRED=NO -$SDK -workspace gaios.xcworkspace -scheme gaios clean build -configuration release
fi
