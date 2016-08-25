Build status: [![Build Status](https://travis-ci.org/greenaddress/GreenBits.png?branch=master)](https://travis-ci.org/greenaddress/GreenBits) 

## Android SDK requirements

You need to have correctly installed the following

- "Android SDK Platform-tools" version 24.0.2 recommended
- "Android SDK Tools" version 25.1.7 recommended
- "Android SDK Build-tools" version 24.0.1 recommended
- "Android Support Library" version 23.2.1 recommended
- "Android Support Repository" version 35.0.0 recommended
- "Android NDK" version r12b recommended

## Clone the repo

`git clone https://github.com/greenaddress/GreenBits.git`

`cd GreenBits`

## How to build

#### Cross-compile the native libraries:

This step requires the environment variables ANDROID_NDK and JAVA_HOME to be set correctly

`cd app && ./prepare_fdroid.sh`

#### Build the Android app

`./gradlew build`

This will build both MAINNET and TESTNET builds

For TESTNET only run `./gradlew assembleBtctestnetDebug`

#### Rebuild the checkpoints (optional)

Start on localhost Bitcoin Core on both MainNet and TestNet and make sure they are synchronized and finished booting (verifying blocks, etc)

MainNet:

`./gradlew --project-dir=bitcoinj/tools buildMainnetCheckpoints && mv bitcoinj/tools/checkpoints app/src/production/assets/checkpoints`

TestNet:

`./gradlew --project-dir=bitcoinj/tools buildTestnetCheckpoints && mv bitcoinj/tools/checkpoints-testnet app/src/btctestnet/assets/checkpoints`

or for both in one go run:

`./buildCheckpoints.sh`

### Acknowledgements

Thanks to [Bitcoin Wallet for Android](https://github.com/schildbach/bitcoin-wallet) for their QR scanning activity source code!

Thanks to [Riccardo Casatta](https://github.com/RCasatta) for code and big UX contributions!
