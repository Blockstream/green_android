# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

### Added
- Reset two factor authentication
- Refactoring restore wallet flow, enabled Liquid Testnet in production, UI fix for iOS15
- App Settings: new option to show/hide testnet selection during onboarding
- Overview: warning card for testnet wallets

### Changed
- Show TEST instead of BTC for a testnet wallet
- Link settings and 2fa data into session to better handle 2fa reset/cancel

### Fixed


## [3.7.3] - 2021-09-30

### Changed
- Updates GDK to 0.0.45.post1

### Fixed
- Explicitly ignore expired certificate in Jade pinserver request

## [3.7.2] - 2021-09-28

### Added
- Support host unblinding for Blockstream Jade version 0.1.27 and higher

### Changed
- New wallet view with revamped UI
- Improves network reconnection behavior
- Updates and supports GDK version 0.0.45
- Updates translations

### Fixed
- Validation of addresses for 2of3 accounts using Blockstream Jade
- Disconnection at auto-logout timeout
- Amounts displayed when sweeping paper wallets

## [3.7.0] - 2021-09-08

### Added
- Support for creating and restoring Singlesig wallets on Bitcoin
- Support for Fastlane to streamline future beta releases

### Changed
- Improves hardware wallet integration
- Updates localizations
- Updates GDK to version 0.0.44

### Fixed
- Fixes UI settings for smaller screens

## [3.6.6] - 2021-08-17

### Added
- Anti-exfil signing protocol support for Blockstream Jade
- Automated tests for onboarding and transactions

### Changed
- Improves Wallet Settings UI
- Preloads icons of Liquid assets
- Improves support for Blockstream Jade hardware

### Fixed
- Title trimming on low resolution devices

## [3.6.3] - 2021-07-13

### Added
- Generates 12 words recovery phrases by default
- Support for creating and restoring Singlesig wallets on Bitcoin Testnet
- Adds account type label in Account Card
- Enhanced support for Blockstream Jade

### Changed
- Removes limit in maximum number of AMP accounts that can be added
- Updates GDK to 0.0.43

## [3.6.1] - 2021-06-18

### Fixed
- Crash on iOS 12
- Checkbox for system message approval
- Bug showing hardware wallets alert when using a software wallet

## [3.6.0] - 2021-06-07

### Added
- Improved UI for 2FA reset using new alert cards
- Users can now undo a 2FA dispute

### Changed
- Improved Blockstream Jade onboarding
- Improved Liquid asset registry loading, supporting refresh in case of failures
- Auto-advance after typing last digit of 2FA codes
- UI improvements for smaller screens
- Updated GDK

### Fixed
- URLs to view transactions on the explorer
