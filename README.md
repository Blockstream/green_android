Build status: [![Build Status](https://travis-ci.org/greenaddress/GreenBits.png?branch=master)](https://travis-ci.org/greenaddress/GreenBits) 

## Android SDK requirements

You need to have correctly installed the following

- "Android SDK Platform-tools" version 22 recommended
- "Android SDK Tools" version 24.2.0 recommended
- "Android SDK Build-tools" version 22.0.1 recommended
- "Android Support Library" version 22.1.1 recommended
- "Android Support Repository" version 14 recommended

## How to build

Simply run `./gradlew build`

This would build both MAINNET and TESTNET builds

For TESTNET only run `./gradlew assembleTestnetDebug`

### Acknowledgements

Thanks to [Bitcoin Wallet for Android](https://github.com/schildbach/bitcoin-wallet) for their QR scanning activity source code!

Thanks to [Riccardo Casatta](https://github.com/RCasatta) for code and big UX contributions!
