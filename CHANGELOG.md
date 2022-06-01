# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

### Fixed
- Improve Ledger support

### Added
- Trezor singlesig address display

## [3.8.5] - 2022-07-15
### Added
- Enable watch-only in Liquid

### Changed
- Update GDK to 0.0.55
- Updated project dependencies
- Display the receive address in Transaction Details
- Display the amount without the fee in Transaction Details

### Fixed
- Fix error message on wrong PIN
- Fix 27-words mnemonic wallet restore
- Fix change address verification on hardware wallets

## [3.8.4] - 2022-06-17
### Changed
- Update GDK to 0.0.54.post1

### Fixed
- Fix balances after bumping transaction fees

## [3.8.3] - 2022-06-09
### Added
- SPV for Bitcoin multisig shield wallets
- Help Green improve! If you agree, Green will collect limited usage data to optimize your experience

### Fixed
- Fix 2FA reset error message even on successful reset
- Fix Singlesig BIP21 amount error
- Fix Ledger address validation on Liquid Singlesig
- Fix default wallet name was empty on rename dialog
- Close drawer when clicking a wallet notification

## [3.8.2] - 2022-05-18
### Added
- Archive accounts if they're no longer needed, then unarchive them from the archived list
- Support multiple hardware wallet devices on multiple networks concurrently

### Changed
- Updated GDK to 0.0.54
- Updated project dependencies

### Fixed
- Crash on Liquid transactions with unblinded amounts
- Crash when resuming the application after prolonged period of inactivity

## [3.8.0] - 2022-04-13
### Added
- Add Bitcoin Singlesig hardware wallet support
- Show active wallet sessions as system notification
- Support Jade firmware sha256 file verification
- Add ability to copy various transaction details
- Change network with hardware wallet from the toolbar icon

### Changed
- Updated GDK to 0.0.51
- Slide to send a transaction

### Fixed
- Fix transaction fees amounts are not converted to fiat currencies
- Fix Trezor passphrase entry with empty string
- Fix WalletRepository being accessed from main thread
- Fix sharing Liquid's confidential and non confidential links


## [3.7.9] - 2022-02-22
### Added
- Romanian localization

### Changed
- Improved transaction review layout both for software and hardware wallets
- Update translations

## [3.7.8] - 2022-01-20
### Added
- Show recovery phrases as Qr codes, to facilitate exporting your wallets on a new device
- 2of3 account creation for Bitcoin multisig shield wallets

### Changed
- Streamlined wallet navigation: switch between your wallets without needing to log out every time
- Show a bottom sheet when an interaction with hardware wallet is needed
- Updated GDK to 0.0.49
- Updated project dependencies

### Fixed
- Jade Bluetooth pairing with Google Pixel devices
- PIN screen layout for larger displays
- Tor connection indicator
- Crash when requesting USB permissions on Android 12

## [3.7.7] - 2021-12-17
### Added
- Automatic wallet restore, Green will find any wallet associated with your recovery phrase
- Enhanced privacy option enabling secure display throughout the app & screen lock
- PIN keyboard shuffle, to augment your privacy when entering PIN on login
- L-BTC and BTC asset details

### Changed
- Improved swifter Send flow, easier to use, easier to read
- Improved sweep paper wallet
- Improved transaction fee bumping
- Added balances on account cards, to facilitate navigation across accounts
- Support for wallet creation with both 12 or 24 words recovery phrases
- Updated GDK to 0.0.48
- Added Android 12 target

### Fixed
- Fix SPV progress indicator

## [3.7.6] - 2021-11-10
### Added
- Support to connect to your personal electrum server in app settings

### Changed
- Supports GDK 0.0.47
- Update project dependencies

### Fixed
- Disconnects Ledger X sessions when on Dashboard app
- Accounts renaming
- Pasting PGP keys in settings
- Biometric authentication for Android 12

## [3.7.5] - 2021-10-26
### Changed
- Update GDK to 0.0.46.post1

## [3.7.4] - 2021-10-22
### Added
- Supports creating and restoring Singlesig wallets on Liquid
- Supports GDK 0.0.46
- New camera Qr-code scanner
- Handles BIP-21 payment URIs opened from other apps (bitcoin: & liquidnetwork:)

### Changed
- Improves wallet restore flow
- Improves transaction details view with a new UI
- Improves sweep paper wallet
- Simplifies "Send to" screen adding a way to quickly paste an address or scan a QR code
- Testnet networks must be enabled from App Settings to appear as create/restore options
- Shows a warning when operating on a testnet network
- No longer passes additional root certificate when fetching files from Jade firmware server

### Fixed
- Read-only amount when bumping transaction fees
- 2FA popup truncated on small screens
- Crash when deleting a wallet
- Trezor One login with Passphrase

## [3.7.3] - 2021-09-30
### Changed
- Bump GDK to version 0.0.45.post1

### Fixed
- Ignore expired server certs in Jade PIN requests

## [3.7.2] - 2021-09-24
### Added
- Add SPV support to singlesig wallets in app settings
- Support host unblinding for Blockstream Jade version 0.1.27 and higher

### Changed
- Revamp Wallet view with new UI
- Minor improvements to the Wallet Settings UI and PIN view
- Update and support GDK version 0.0.45

### Fixed
- Limit number of words in wallet restore to 27
- Fix crash reported on Play Store
- Fix bugs in hardware wallets support

## [3.7.1] - 2021-09-16
### Fixed
- Fix GDK build for f-droid

## [3.7.0] - 2021-09-03
### Added
- Support for creating and restoring singlesig wallets on Bitcoin

### Changed
- Update GDK to version 0.0.44
- Update Gradle dependencies

## [3.6.4] - 2021-07-23
### Added
- Adds a PIN pad for 2FA codes

### Changed
- Improves Wallet Settings UI
- Drops support for bitcoinj and SPV on Multisig Shield wallets

### Fixed
- Fixes crashes with fingerprint login authentication
- Shows PGP and Watch-Only settings only on Multisig Shield wallets

## [3.6.3] - 2021-07-12
### Added
- Support for creating and restoring Singlesig wallets on Bitcoin Testnet
- Enhanced support for Blockstream Jade

### Changed
- Improves address validation on hardware wallets
- Preloads icons of Liquid Assets
- Improves handling of disconnection and reconnection
- Adds a warning when opening Help Center articles while using Tor
- Updates GDK to 0.0.43

### Fixed
- Ledger NanoX devices device identification
- Bluetooth device discovery
- Decimal and fiat amount request on receive
- Crash on Watch-Only wallets
- Crash when resuming the application after prolonged period of inactivity

## [3.6.0] - 2021-05-31
### Added
- Show the wallet name in the main view
- Cache liquid asset registry in addition to GDK caching
- Display the account type in the account cards
- Improve tools for testing
- Revamp receive view with new UI and button to verify addresses on hardware wallets

### Changed
- Generate 12 words recovery phrases by default
- Update Android and GDK dependencies

### Fixed
- Fix error handling when restoring or creating wallets
- Fix migration from v2 android screenlock logins
