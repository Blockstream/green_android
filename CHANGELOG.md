# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
 
## Unreleased

### Added
- Create sub accounts of type 2of3
- Settings: show mnemonic as QR code

### Changed
- Select mnemonic length while restoring wallet
- Access app settings screen from hardware wallet connecting screen
- Converted send transaction button to "slide to send" control
- Update GDK to version 0.50
- Refactoring asset detail screen

### Fixed
- Fix session autologout on background after timeout

## [3.7.9] - 2022-02-15

### Added
- Streamlined wallet navigation: switch between your wallets without needing to log out every time
- Romanian localization

### Changed
- Improved swifter Send flow, easier to use, easier to read
- Enabled Liquid testnet for Jade when in test mode
- Restoring an already available wallet is detected, preventing duplicates

## [3.7.8] - 2022-01-20

### Changed
- Update GDK to version 0.0.49
- Improve automatic wallet restore

## [3.7.7] - 2021-12-17

### Added
- Automatic wallet restore, Green will find any wallet associated with your recovery phrase
- Balances on account cards when switching between accounts
- Support for wallet creation with both 12 or 24 words recovery phrases
- Support for Ledger Nano X firmware 2.0.0

### Changed
- Improved transaction details layout
- Updated GDK to 0.0.48

### Fixed
- Assist Jade users with Bluetooth re-pairing after firmware 0.1.31+ upgrade

## [3.7.6] - 2021-11-10
### Added
- Support for send to bech32m P2TR address types, available 144 blocks after Taproot activation
- Support to connect to your personal electrum server, available in app settings for singlesig wallets 
- Support to validate transaction inclusion in a block (SPV), available in app settings for singlesig wallets 

### Changed
- Revamps receive view with new UI and button to verify addresses on hardware wallets
- Supports GDK 0.0.47

### Fixed
- Loading of trasactions in the home view
- URL with unblinding data for Liquid transactions
- Improves BLE scanning and error messages

## [3.7.5] - 2021-10-27

### Added
- Supports creating and restoring Singlesig wallets on Liquid
- Reset two factor authentication
- SPV header validation for transactions validation

### Changed
- Improves wallet restore flow
- Testnet networks must be enabled from App Settings to appear as create/restore options
- Testnet UI clarifies that funds have no value on these wallets
- Prompts to perform Jade OTA firmware upgrades via USB cable
- Shows a warning when operating on a testnet network
- Updates GDK to 0.0.46.post1

### Fixed
- Uses default minimum fees when estimates are not available
- UI on restore for iOS15
- Updates fastlane flags on debug mode

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
