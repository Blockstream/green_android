# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [4.0.35] - 2024-09-26

### Added
- Verify address with HWW upon 2FA reactivation

### Changed
- Bump GDK to version 0.73.1

## [4.0.34] - 2024-09-04

### Changed
- Update dependencies
 
- ### Fixed
- Various bug fixes

## [4.0.33] - 2024-08-15

- ### Fixed
- Fix GDK build for F-Droid

## [4.0.32] - 2024-08-12

### Added
- Allow custom gap limit on personal Electrum server
- Allow adding a comment on a LNURL payment

### Changed
- Refactor Wallet overview options menu
- Bump GDK to version 0.72.2
- Bump Breez to version 0.5.2
 
- ### Fixed
- Various bug fixes

## [4.0.31] - 2024-07-02

### Added
- Allow disabling TLS on Personal Electrum servers

### Changed
- Refactor ViewModels to support Kotlin Multiplatform
- Refactor various UI elements
- Updated project dependencies
- Bump Breez to version 0.5.1-rc4

- ### Fixed
- Fix F-Droid dependency issue

## [4.0.30] - 2024-06-10

### Added
- Re-enable expired 2FA UTXOs
 
### Changed
- Update to Kotlin 2.0.0
- Updated project dependencies
- Bump GDK to version 0.71.3
- Bump Breez to version 0.4.2-rc1
- Hide fee selection on Liquid if fee estimation is network default
- Refactor various UI elements

### Fixed
- Fix Authenticator app 2FA setup

## [4.0.29] - 2024-05-23

### Changed
- Bump Breez to version 0.4.1

### Fixed
- Fix F-Droid dependency issue

## [4.0.28] - 2024-05-13

### Added
- Revamp Sweep UI

### Changed
- Bump GDK to version 0.71.0
- Bump Breez to version 0.4.1-rc2

### Fixed
- Fix v2 password login
- Fix wrong conversion when generating an address with amount

## [4.0.27] - 2024-03-28

### Added
- Add push notification support for Lightning payments

### Changed
- Bump Breez to version 0.3.8
- Make Lightning Shortcut opt-out
- Update transaction details view
- Improve QR code scanability

## [4.0.26] - 2024-03-18

### Changed
- Bump Breez to version 0.3.6

## [4.0.25] - 2024-03-15

### Added
- Enable Singlesig Liquid watch-only descriptors
- New app setting "Remember hardware device"
- Add metadata for F-Droid build
- Handle "master blinding key" for advanced jade users

### Changed
- Bump GDK to version 0.70.3
- Allow changing denomination on watch-only sessions
- Rewrite some UI screens into Jetpack Compose
- Enable Bluetooth adapter from the app
- Change Assets cache behavior
- Enable Sweep for Bitcoin Singlesig watch-only sessions

### Fixed
- Fix F-Droid build
- Various bug fixes

## [4.0.24] - 2024-01-31

### Added
- Export Greenlight logs
- Add "Receive any Liquid/AMP asset" to the assets list on Receive and Create New Account screens

### Changed
- 2FA SMS activation comply with US requirements
- Allow setting custom fees when recovering funds from Lightning
- Refactor ViewModels to support Kotlin Multiplatform
- Rewrite some UI screens into Jetpack Compose
- Update Jade Oracle whitelisted urls
- Bump Breez to version 0.2.15
- Bump GDK to version 0.70.0

### Fixed
- Fix F-Droid build
- Fix message signing UI
- Various bug fixes

## [4.0.23] - 2024-01-08

### Changed
- Updated project dependencies
- Improve build reproducibility

### Fixed
- Various bug and crash fixes

## [4.0.22] - 2023-12-19

### Changed
- Add a NoCountly no-op class to FDroid build
- Updated project dependencies
- Bump Breez to version 0.2.12

### Fixed
- Fix emergency recovery phrase restore
- Fix Account Overview crash

## [4.0.21] - 2023-12-06

### Fixed
- Fix logout from the Notifications menu displaying the autologout toast
- Check watch-only wallet if already exists in the app
- Various bug and crash fixes

## [4.0.20] - 2023-11-23

### Fixed
- Fix keystore key being invalidated when device was unlocked with face biometrics

## [4.0.19] - 2023-11-20

### Changed
- Bump GDK to version 0.69

### Added
- Support Lightning with Jade air-gapped BIP85 mnemonic
- Support Jade watch-only import by scanning BCUR animated QR codes
- Enable 2FA call method as an SMS backup

### Fixed
- Fix amount being calculated incorrectly for locales using comma as decimal separator
- Various bug and crash fixes

## [4.0.18] - 2023-11-06

### Fixed
- Fix Jade BLE connection on Android 14

## [4.0.17] - 2023-10-30

### Changed
- Bump GDK to version 0.68.4
- Bump Breez to version 0.2.7

### Added
- Add icons to menu items
- Display an error message when personal electrum server is unreachable

### Fixed
- Fix scan of color inverted QR codes
- Fix NPE crash related to session handling
- Fix fetching remote config when Tor is enabled

## [4.0.15] - 2023-10-11

### Changed
- Bump Breez to version 0.2.5
- Support Android 14

### Added
- Lightning Shortcut

### Fixed
- Minor UI improvements

## [4.0.13] - 2023-09-20

### Changed
- Bump GDK to version 0.67.1

### Added
- Jade: Warning for non-default PIN server
- Support for message signing

### Fixed
- Bug fixes

## [4.0.12] - 2023-08-14

### Changed
- Support Lightning channel closure and sweep funds to onchain address

### Added
- UI improvements to lightning accounts

## [4.0.11] - 2023-07-27

### Changed
- Bump GDK to version 0.0.65
- Bump Breez to version 0.1.4
- Set Native SegWit as default standard account

### Added
- Add lightning onchain deposits

### Fixed
- Prompt for Jade FW update

## [4.0.10] - 2023-07-05

### Fixed
- Prompt for Jade FW update

## [4.0.9] - 2023-06-16

### Changed
- Make QR code scanner take 85% of the screen
- Long press a QR code to show it full screen

### Fixed
- Remove PIN character limit for Trezor One
- Hide Denomination & Exchange dialog for watch-only multisig
- Fix 2FA threshold with empty string
- Fixes on denomination behaviors
- Fix receive address ellipsize middle

## [4.0.8] - 2023-06-09

### Changed
- Move "Recovery Transactions" setting in "Two-Factor authentication"

### Fixed
- Fix login into Bitcoin Multisig Watch-only wallet

## [4.0.7] - 2023-06-08

### Changed
- Bump GDK to version 0.0.63
- Update dependencies

### Added
- Add lightning support as experimental feature
- Add a blinding step in liquid for software and hardware wallets

### Fixed
- Fix watch-only view for large fonts

## [4.0.4] - 2023-05-03

### Changed
- Bump GDK version to 0.0.62
 
## [4.0.3] - 2023-04-29

### Added
- Enable watch-only for Singlesig Bitcoin

### Fixed
- Bug fixes
- Fix F-Droid build

## [4.0.1] - 2023-04-19

### Changed
- Bump GDK version to 0.0.61

## [4.0.0] - 2023-04-16

### Fixed
- Fix 2FA reset procedure

## [4.0.0-beta5] - 2023-04-06

### Added
- Animated visuals

### Fixed
- Fix custom electrum server with Tor enabled
- Fix crash when app returns from the background and GDK session is no longer available

## [4.0.0-beta4] - 2023-03-07

### Added
- Add troubleshooting links in Jade onboarding
- Persist last selection of the "Remember device" option 

### Fixed
- Fix Jade login with bip39 passphrases
- Fix Trezor signing to recognize change address when sending

## [4.0.0-beta3] - 2023-02-14

### Changed
- Lock Jade on session logout

### Fixed
- Fix device list showing previously connected devices
- Hide menu entries(rename/archive) for watch only sessions
- Show the change address in Verification bottom sheet on Trezor devices
- Fix wallet restore when the corresponding multisig watch-only is already present in the wallets list
- Disable amount entry points until a valid address is entered
- Show fee error on bump transaction fee

## [4.0.0-beta2] - 2023-02-07

### Changed
- New Hardware Wallet Onboarding

## [4.0.0-beta1] - 2023-02-06

### Changed
- Network Unification
- Update GDK to 0.0.58

## [3.9.2] - 2023-01-17

### Added
- Added CSAT forms

## [3.9.1] - 2022-12-15

### Fixed
- Fix change address shown as recipient address
- Fix SPV icon showing an error for unconfirmed transactions

## [3.9.0] - 2022-11-23

### Added
- Emergency recovery phrase restore

### Changed
- Update GDK to 0.0.57

## [3.8.9] - 2022-10-07

### Added
- Add 2FA reset option for multisig shield Liquid wallets

### Changed
- Update GDK to 0.0.56

### Fixed
- Incorrectly displaying "insufficient funds" when sending from Jade singlesig wallet

## [3.8.7] - 2022-09-01

### Added
- New About screen with social links
- Show announcements alerts
- Add BIP info on account labels

### Fixed
- Improve ephemeral BIP39 Passphrase based wallets
- Improve errors during restore
- Fix Jade login using Emergency Restore
- Fix BIP21 prefix for liquid testnet

## [3.8.6] - 2022-07-29

### Added
- Login with BIP39 Passphrase
- Trezor & Ledger singlesig address display
- Faster Jade firmware update with binary delta

### Fixed
- Improve Ledger support

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
