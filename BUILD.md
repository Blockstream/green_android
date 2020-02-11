# Build Blockstream Green for iOS

## Build requirements

Install Xcode.

Get the command line tools with: (ensure to use "Software Update" to install updates)

`sudo xcode-select --install`


Make sure `xcode-select --print-path` returns `/Applications/Xcode.app/Contents/Developer` . Otherwise run:


`sudo xcode-select --switch /Applications/Xcode.app/Contents/Developer`


On macOS 10.14 Mojave, you need an additional step after installing the command line tools:


`sudo installer -pkg /Library/Developer/CommandLineTools/Packages/macOS_SDK_headers_for_macOS_10.14.pkg -target /`


## Clone the repo

```
git clone https://github.com/Blockstream/green_ios.git
cd green_ios
```

## How to build


#### Use the released native library (recommended):

Fetch the latest released gdk binaries (our cross-platform wallet library) with the following command:

`./tools/fetch_gdk_binaries.sh`

You can also cross compile it from source.


#### Cross-compile the native library (advanced):

Get sources from the GDK repository

```
git clone https://github.com/Blockstream/gdk.git
cd gdk
```

Build GDK dependencies for Mac OSX (virtualenv is optional if you already have python3 as default)

```
brew update && brew install ninja automake autoconf libtool gnu-sed python3 wget pkg-config swig gnu-getopt gnu-tar
pip3 install virtualenv
virtualenv -p python3 ./venv
source ./venv/bin/activate
pip install meson
```

Build the library for iphone:

`./tools/build.sh --iphone static --lto=true --install $PWD/gdk-iphone`

or for iphone simulators:

`./tools/build.sh --iphonesim static --lto=true --install $PWD/gdk-iphone`


#### CocoaPods requirements

Install CocoaPods dependencies:

`pod install`


#### Install the app

Open the project with Xcode and hit Play.



