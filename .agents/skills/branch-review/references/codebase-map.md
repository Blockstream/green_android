# Codebase Map

Orientation for reviewing branches of this repository. Gathered from the default branch (`origin/HEAD`) at `0aecc3048` (2026-10-02). Refresh it with the `update-agent-docs` skill after architecture changes, not during a review.

## Product Summary

Blockstream App is a non-custodial Bitcoin wallet built around GDK. It supports Bitcoin, Liquid, multisig security features, watch-only wallets, hardware wallets such as Jade, Lightning, send/receive, swaps, transaction history, settings, and wallet recovery/onboarding flows. Android is the shipped app; `desktopApp` is a Compose Desktop target.

## Module Layout

Gradle modules from `settings.gradle.kts`:

- `androidApp`: Android application shell: activity, application, Koin Android modules, deep links, WorkManager workers, FCM handling, and the Room-to-SQLDelight migrator. Flavors: `development`, `productionGoogle`, `productionFDroid`.
- `compose`: shared Compose UI: screens, bottom sheets, dialogs, components, navigation and ViewModels (`models/`). Source sets `commonMain`, `androidMain`, `desktopMain`.
- `designsystem`: `AppTheme`, color and typography tokens, and the bundled fonts.
- `data`: wallet/session managers, the Kotlin GDK and Wally wrappers (`data/.../gdk/`), network backends (GDK, LWK, Greenlight), SQLDelight persistence, settings, assets, Lightning, LWK, swaps, Meld, notifications, Zendesk support tickets and the Jade wallet integration. Source sets `commonMain`, `androidMain`, `jvmMain`.
- `domain`: use cases for wallet, account, send, receive, transaction, swap, hardware, Meld, notifications and other app workflows. `DomainModule.kt` includes the feature modules (`send/SendModule.kt`, `swap/SwapModule.kt`, ...) and registers the remaining use cases itself.
- `gdk`: Android library that ships the GDK native libraries. Its `fetchAndroidBinaries` task downloads them along with the SWIG Java bindings (`GDK.java`, `Wally.java`); all of these are gitignored.
- `jade`: Kotlin Multiplatform Jade client: `JadeAPI`, CBOR request/response types (`api/`), connections (`connection/`) and firmware updates (`firmware/`).
- `hardware`: Android-only Ledger (`com.btchip`) and Trezor support.
- `network`: shared `AppHttpClient`.
- `utils`: logging utilities.
- `base-gms`, `gms`, `no-gms`: store variants. `base-gms` holds open `GooglePlay` and `InstallReferrer` classes with no-op defaults, and `data` holds the matching `fcm/Firebase` class. `gms` (development and Google flavors) overrides all three; `no-gms` (F-Droid) binds the no-op classes.
- `desktopApp`: Compose Desktop entry points (`Main.kt`, `DevMain.kt`).

Module dependencies run `androidApp` → `compose` → `domain` → `data`. `data` depends on `jade`, `network` and `utils`, and on `gdk` on Android only; `network` depends on `utils`. `compose` also depends on `designsystem` and, on Android, on `hardware` and `base-gms`. `hardware` depends directly on `data` and `gdk`, so GDK and Wally changes reach its Ledger and Trezor code. `base-gms` depends on `data`, `gms` on `base-gms` and `data`, and `no-gms` on `base-gms`; `androidApp` picks `gms` or `no-gms` per flavor, and `desktopApp` depends on `compose`.

## Core Runtime Areas

Wallet/session behavior:

- `data/src/commonMain/kotlin/com/blockstream/data/managers/SessionManager.kt`
- `data/src/commonMain/kotlin/com/blockstream/data/gdk/GdkSession.kt`
- `data/src/commonMain/kotlin/com/blockstream/data/backend/NetworkBackend.kt`
- `data/src/commonMain/kotlin/com/blockstream/data/backend/AccountBackend.kt`
- `data/src/commonMain/kotlin/com/blockstream/data/backend/gdk/GdkNetworkBackend.kt`
- `data/src/commonMain/kotlin/com/blockstream/data/backend/gdk/GdkAccountBackend.kt`
- `data/src/commonMain/kotlin/com/blockstream/data/backend/gl/GlNetworkBackend.kt`
- `data/src/commonMain/kotlin/com/blockstream/data/backend/lwk/LwkNetworkBackend.kt`

Each network backend implements `NetworkBackend` and exposes `AccountBackend`s: `gdk/` for GDK Bitcoin and Liquid accounts, `lwk/` for LWK Liquid accounts, `gl/` for Greenlight Lightning. The SDK wrappers sit beside them:

- GDK and Wally: `data/src/commonMain/kotlin/com/blockstream/data/gdk/` (`Gdk.kt`, `Wally.kt`, `AuthHandler.kt`), with `Gdk.android.kt` / `Gdk.jvm.kt` actuals.
- LWK and Boltz swaps: `data/src/commonMain/kotlin/com/blockstream/data/lwk/`, `data/src/commonMain/kotlin/com/blockstream/data/swap/`.
- Lightning (Greenlight): `data/src/commonMain/kotlin/com/blockstream/data/lightning/`.
- Jade wallet integration: `data/src/commonMain/kotlin/com/blockstream/data/jade/JadeHWWallet.kt`.

Persistence:

- SQLDelight schemas: `data/src/commonMain/database_wallet/` (`WalletDB`: wallets, login credentials, wallet settings, LWK accounts, Boltz swaps) and `data/src/commonMain/database_local/` (`LocalDB`: events), each with a `migrations/` folder of `.sqm` files.
- `data/src/commonMain/kotlin/com/blockstream/data/database/Database.kt`
- Key-value settings: `data/src/commonMain/kotlin/com/blockstream/data/managers/SettingsManager.kt`, `WalletSettingsManager.kt`.
- The Android-only Room database in `androidApp/src/main/java/com/blockstream/green/database/` is read by `green/settings/AndroidMigrator.kt` to migrate older installs into SQLDelight.

Dependency wiring:

- `data/src/commonMain/kotlin/com/blockstream/data/di/Koin.kt`
- `data/src/commonMain/kotlin/com/blockstream/data/DataModule.kt`
- `domain/src/commonMain/kotlin/com/blockstream/domain/DomainModule.kt`
- feature modules such as `domain/.../*Module.kt`
- `compose/src/commonMain/kotlin/com/blockstream/compose/di/Koin.kt`
- Android: `androidApp/src/main/java/com/blockstream/green/di/` (`KoinAndroid.kt`, `GreenModules.kt`, `ViewModels.kt`); desktop: `compose/src/desktopMain/kotlin/com/blockstream/compose/di/KoinDesktop.kt`

Platform code uses `expect`/`actual`: actuals are named `*.android.kt` (`androidMain`), `*.jvm.kt` (`data` `jvmMain`) and `*.desktop.kt` (`compose` `desktopMain`). A change to an `expect` declaration needs every actual updated.

Background work and notifications (Android):

- WorkManager workers: `androidApp/src/main/java/com/blockstream/green/work/` (`BoltzWork.kt`, `LightningWork.kt`, `MeldPendingTransactionsWorker.kt`)
- FCM: `gms/src/main/java/com/blockstream/gms/services/FirebaseMessagingService.kt`, `data/src/commonMain/kotlin/com/blockstream/data/fcm/`, `data/src/commonMain/kotlin/com/blockstream/data/notifications/`
- Deep links: `androidApp/src/main/java/com/blockstream/green/utils/DeepLinkHandler.kt`

App navigation and common ViewModel behavior:

- `compose/src/commonMain/kotlin/com/blockstream/compose/GreenApp.kt`
- `compose/src/commonMain/kotlin/com/blockstream/compose/navigation/Destinations.kt`
- `compose/src/commonMain/kotlin/com/blockstream/compose/models/GreenViewModel.kt`

## High-Value User Flows And Entry Points

In `compose/src/commonMain/kotlin/com/blockstream/compose/`, each feature has a ViewModel in `models/<feature>/` and its UI in `screens/<feature>/`, `sheets/` or `dialogs/`; Android-specific UI is in the matching `compose/src/androidMain` packages.

Login, restore, auto-login, biometrics, watch-only and hardware login:

- `compose/src/commonMain/kotlin/com/blockstream/compose/models/login/LoginViewModel.kt`
- `compose/src/commonMain/kotlin/com/blockstream/compose/models/home/HomeViewModel.kt`
- `compose/src/commonMain/kotlin/com/blockstream/compose/models/onboarding/SetupNewWalletViewModel.kt`
- `compose/src/commonMain/kotlin/com/blockstream/compose/models/onboarding/phone/EnterRecoveryPhraseViewModel.kt`
- `compose/src/commonMain/kotlin/com/blockstream/compose/models/devices/DeviceListViewModel.kt`
- `compose/src/commonMain/kotlin/com/blockstream/compose/models/devices/DeviceScanViewModel.kt`
- `compose/src/commonMain/kotlin/com/blockstream/compose/models/jade/JadeQRViewModel.kt`

Wallet overview, balances, assets and tabs:

- `compose/src/commonMain/kotlin/com/blockstream/compose/models/overview/WalletBalanceViewModel.kt`
- `compose/src/commonMain/kotlin/com/blockstream/compose/models/overview/WalletOverviewViewModel.kt`
- `compose/src/commonMain/kotlin/com/blockstream/compose/models/overview/TransactViewModel.kt`
- `compose/src/commonMain/kotlin/com/blockstream/compose/models/assetaccounts/AssetAccountDetailsViewModel.kt`
- `compose/src/commonMain/kotlin/com/blockstream/compose/models/assetaccounts/AssetAccountListViewModel.kt`

Send, receive, invoices, swaps and transaction confirmation:

- `compose/src/commonMain/kotlin/com/blockstream/compose/models/send/SendViewModel.kt`
- `compose/src/commonMain/kotlin/com/blockstream/compose/models/receive/ReceiveViewModel.kt`
- `compose/src/commonMain/kotlin/com/blockstream/compose/models/receive/ReceiveChooseAssetViewModel.kt`
- `compose/src/commonMain/kotlin/com/blockstream/compose/models/receive/ReceiveChooseAccountViewModel.kt`
- `compose/src/commonMain/kotlin/com/blockstream/compose/models/transaction/TransactionViewModel.kt`
- `domain/src/commonMain/kotlin/com/blockstream/domain/send/`
- `domain/src/commonMain/kotlin/com/blockstream/domain/receive/`
- `domain/src/commonMain/kotlin/com/blockstream/domain/swap/`
- `compose/src/commonMain/kotlin/com/blockstream/compose/models/swap/SwapViewModel.kt`

Lightning, LNURL and Buy:

- `compose/src/commonMain/kotlin/com/blockstream/compose/models/lightning/`
- `compose/src/commonMain/kotlin/com/blockstream/compose/models/exchange/BuyViewModel.kt`
- `domain/src/commonMain/kotlin/com/blockstream/domain/meld/`

Settings, two-factor, watch-only credentials and recovery phrase backup:

- `compose/src/commonMain/kotlin/com/blockstream/compose/models/settings/`
- `compose/src/commonMain/kotlin/com/blockstream/compose/models/recovery/`

Wallet data use cases:

- `domain/src/commonMain/kotlin/com/blockstream/domain/base/ObservableUseCase.kt`
- `domain/src/commonMain/kotlin/com/blockstream/domain/base/CachedObservableUseCase.kt`
- `domain/src/commonMain/kotlin/com/blockstream/domain/transaction/GetWalletTransactionsUseCase.kt`
- `domain/src/commonMain/kotlin/com/blockstream/domain/account/`
- `domain/src/commonMain/kotlin/com/blockstream/domain/wallet/`

Settings, formatting, assets and policy-asset helpers:

- `data/src/commonMain/kotlin/com/blockstream/data/utils/Conversions.kt`
- `data/src/commonMain/kotlin/com/blockstream/data/extensions/GdkExtensions.kt`
- `data/src/commonMain/kotlin/com/blockstream/data/data/EnrichedAsset.kt`
- `data/src/commonMain/kotlin/com/blockstream/data/managers/NetworkAssetManager.kt`
- `data/src/commonMain/kotlin/com/blockstream/data/managers/AssetManager.kt`

## Review Heuristics For This App

Pay special attention to:

- GDK calls made from UI collectors or main-thread contexts.
- Logout/disconnect races, especially late backend notifications during session teardown.
- Whether flows preserve cancellation behavior and rethrow `CancellationException`.
- Whether errors become silent partial data, stale data or misleading placeholders.
- Whether Home, Transact, account views, send/receive and asset details share a canonical source of balance/asset state.
- Whether Koin scopes are `single`, `factory` or per-session in ways that create separate caches.
- Whether Liquid, Bitcoin, Lightning and policy assets are mapped to the correct network/account.
- Whether watch-only, Jade, multisig, singlesig, Liquid, Lightning and restore paths still use compatible session/backend assumptions.
- Whether a SQLDelight schema change comes with a `.sqm` migration, so existing installs upgrade without losing wallets or login credentials.
- Whether a change to an `expect` declaration or an Android-only code path still builds and behaves on the desktop target.
