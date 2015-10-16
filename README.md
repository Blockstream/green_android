Build status: [![Build Status](https://travis-ci.org/greenaddress/GreenBits.png?branch=master)](https://travis-ci.org/greenaddress/GreenBits) 

## Android SDK requirements

You need to have correctly installed the following

- "Android SDK Platform-tools" version 23 recommended
- "Android SDK Tools" version 24.3.3 recommended
- "Android SDK Build-tools" version 23.0.1 (23.0.0 needed for material-dialog) recommended
- "Android Support Library" version 22.1.2 recommended
- "Android Support Repository" version 17 recommended

## Clone the repo and init submodules

`git clone https://github.com/greenaddress/GreenBits.git`

`cd GreenBits`

`git submodule update --init --recursive`

## How to build

Simply run `./gradlew build`

This would build both MAINNET and TESTNET builds

For TESTNET only run `./gradlew assembleBtctestnetDebug`

### Acknowledgements

Thanks to [Bitcoin Wallet for Android](https://github.com/schildbach/bitcoin-wallet) for their QR scanning activity source code!

Thanks to [Riccardo Casatta](https://github.com/RCasatta) for code and big UX contributions!
