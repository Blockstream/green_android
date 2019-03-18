# Green - A native Blockstream wallet for iOS


## Clone the repo

```
git clone https://github.com/Blockstream/green_ios.git
cd green_ios
```

## Build requirements

### Global requirements

Install Xcode.

Get the command line tools, or ensure they are up to date

`xcode-select --install`

Make sure `xcode-select -p` returns `/Applications/Xcode.app/Contents/Developer` . Otherwise run:

`xcode-select -s /Applications/Xcode.app/Contents/Developer`

On macOS 10.14 Mojave, you have to run another step after installing the command line tools:

`installer -pkg /Library/Developer/CommandLineTools/Packages/macOS_SDK_headers_for_macOS_10.14.pkg -target /`


### Local requirements

Install CocoaPods dependencies locally

`pod install`

## Build GDK for Mac OSX

Get sources from GDK repository
```
git clone https://github.com/Blockstream/gdk.git
cd gdk
```

Build GDK dependencies for Mac OSX (virtualenv optional if you already have python3 as default)
```
brew update && brew install ninja automake autoconf libtool gnu-sed python3 wget pkg-config swig gnu-getopt gnu-tar
pip3 install virtualenv
virtualenv -p python3 ./venv
source ./venv/bin/activate
pip install meson
```

Build for physical device
```
./tools/build.sh --iphone static
```

Build for IPhone simulator
```
./tools/build.sh --iphonesim static
```

Deactivate virtualenv
```
deactivate
cd ..
```

#### Contribution guidelines

See CONTRIBUTING.md for contribution guidelines
