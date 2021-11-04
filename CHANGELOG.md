# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]
### Added

### Changed

### Fixed
- Disconnect Ledger X when on Dashboard app

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
