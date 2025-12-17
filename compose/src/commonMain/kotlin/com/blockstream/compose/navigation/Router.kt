package com.blockstream.compose.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.toRoute
import com.blockstream.compose.dialogs.TorWarningDialog
import com.blockstream.compose.dialogs.UrlWarningDialog
import com.blockstream.compose.managers.rememberStateKeeperFactory
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.models.MainViewModel
import com.blockstream.compose.models.SimpleGreenViewModel
import com.blockstream.compose.models.about.AboutViewModel
import com.blockstream.compose.models.add.Account2of3ViewModel
import com.blockstream.compose.models.add.ChooseAccountTypeViewModel
import com.blockstream.compose.models.add.ReviewAddAccountViewModel
import com.blockstream.compose.models.add.XpubViewModel
import com.blockstream.compose.models.addresses.AddressesViewModel
import com.blockstream.compose.models.addresses.SignMessageViewModel
import com.blockstream.compose.models.archived.ArchivedAccountsViewModel
import com.blockstream.compose.models.assetaccounts.AccountDescriptorViewModel
import com.blockstream.compose.models.assetaccounts.AssetAccountDetailsViewModel
import com.blockstream.compose.models.assetaccounts.AssetAccountListViewModel
import com.blockstream.compose.models.camera.CameraViewModel
import com.blockstream.compose.models.devices.DeviceInfoViewModel
import com.blockstream.compose.models.devices.DeviceListViewModel
import com.blockstream.compose.models.devices.DeviceScanViewModel
import com.blockstream.compose.models.devices.ImportPubKeyViewModel
import com.blockstream.compose.models.devices.JadeGenuineCheckViewModel
import com.blockstream.compose.models.devices.JadeGuideViewModel
import com.blockstream.compose.models.exchange.AccountExchangeViewModel
import com.blockstream.compose.models.exchange.BuyViewModel
import com.blockstream.compose.models.exchange.OnOffRampsViewModel
import com.blockstream.compose.models.home.HomeViewModel
import com.blockstream.compose.models.jade.JadeQRViewModel
import com.blockstream.compose.models.lightning.LnUrlAuthViewModel
import com.blockstream.compose.models.lightning.LnUrlWithdrawViewModel
import com.blockstream.compose.models.lightning.RecoverFundsViewModel
import com.blockstream.compose.models.login.Bip39PassphraseViewModel
import com.blockstream.compose.models.login.LoginViewModel
import com.blockstream.compose.models.onboarding.SetupNewWalletViewModel
import com.blockstream.compose.models.onboarding.hardware.UseHardwareDeviceViewModel
import com.blockstream.compose.models.onboarding.phone.EnterRecoveryPhraseViewModel
import com.blockstream.compose.models.onboarding.phone.PinViewModel
import com.blockstream.compose.models.onboarding.watchonly.WatchOnlyMultisigViewModel
import com.blockstream.compose.models.onboarding.watchonly.WatchOnlySinglesigViewModel
import com.blockstream.compose.models.overview.AccountOverviewViewModel
import com.blockstream.compose.models.overview.SecurityViewModel
import com.blockstream.compose.models.overview.TransactViewModel
import com.blockstream.compose.models.overview.WalletAssetsViewModel
import com.blockstream.compose.models.overview.WalletOverviewViewModel
import com.blockstream.compose.models.promo.PromoViewModel
import com.blockstream.compose.models.receive.ReceiveChooseAccountViewModel
import com.blockstream.compose.models.receive.ReceiveChooseAssetViewModel
import com.blockstream.compose.models.receive.ReceiveViewModel
import com.blockstream.compose.models.recovery.RecoveryCheckViewModel
import com.blockstream.compose.models.recovery.RecoveryIntroViewModel
import com.blockstream.compose.models.recovery.RecoveryPhraseViewModel
import com.blockstream.compose.models.recovery.RecoverySuccessViewModel
import com.blockstream.compose.models.recovery.RecoveryWordsViewModel
import com.blockstream.compose.models.send.BumpViewModel
import com.blockstream.compose.models.send.DenominationViewModel
import com.blockstream.compose.models.send.FeeViewModel
import com.blockstream.compose.models.send.RedepositViewModel
import com.blockstream.compose.models.send.SendAddressViewModel
import com.blockstream.compose.models.send.SendChooseAccountViewModel
import com.blockstream.compose.models.send.SendChooseAssetViewModel
import com.blockstream.compose.models.send.SendConfirmViewModel
import com.blockstream.compose.models.send.SendViewModel
import com.blockstream.compose.models.send.SweepViewModel
import com.blockstream.compose.models.settings.AppSettingsViewModel
import com.blockstream.compose.models.settings.TwoFactorAuthenticationViewModel
import com.blockstream.compose.models.settings.TwoFactorSetupViewModel
import com.blockstream.compose.models.settings.WalletSettingsSection
import com.blockstream.compose.models.settings.WalletSettingsViewModel
import com.blockstream.compose.models.settings.WatchOnlyCredentialsSettingsViewModel
import com.blockstream.compose.models.settings.WatchOnlyViewModel
import com.blockstream.compose.models.sheets.AnalyticsViewModel
import com.blockstream.compose.models.sheets.AssetDetailsViewModel
import com.blockstream.compose.models.sheets.JadeFirmwareUpdateViewModel
import com.blockstream.compose.models.sheets.LightningNodeViewModel
import com.blockstream.compose.models.sheets.MeldCountriesViewModel
import com.blockstream.compose.models.sheets.NoteViewModel
import com.blockstream.compose.models.sheets.RecoveryHelpViewModel
import com.blockstream.compose.models.sheets.TransactionDetailsViewModel
import com.blockstream.compose.models.support.SupportViewModel
import com.blockstream.compose.models.swap.SwapViewModel
import com.blockstream.compose.models.transaction.TransactionViewModel
import com.blockstream.compose.models.twofactor.ReEnable2FAViewModel
import com.blockstream.compose.models.wallet.WalletDeleteViewModel
import com.blockstream.compose.models.wallet.WalletNameViewModel
import com.blockstream.compose.navigation.bottomsheet.onDismissRequest
import com.blockstream.compose.navigation.dialogs.GenericDialog
import com.blockstream.compose.screens.HomeScreen
import com.blockstream.compose.screens.about.AboutScreen
import com.blockstream.compose.screens.add.Account2of3Screen
import com.blockstream.compose.screens.add.ChooseAccountTypeScreen
import com.blockstream.compose.screens.add.ReviewAddAccountScreen
import com.blockstream.compose.screens.add.XpubScreen
import com.blockstream.compose.screens.addresses.AddressesScreen
import com.blockstream.compose.screens.archived.ArchivedAccountsScreen
import com.blockstream.compose.screens.assetaccounts.AccountDescriptorScreen
import com.blockstream.compose.screens.assetaccounts.AssetAccountDetailsScreen
import com.blockstream.compose.screens.assetaccounts.AssetAccountListScreen
import com.blockstream.compose.screens.devices.DeviceInfoScreen
import com.blockstream.compose.screens.devices.DeviceListScreen
import com.blockstream.compose.screens.devices.DeviceScanScreen
import com.blockstream.compose.screens.devices.ImportPubKeyScreen
import com.blockstream.compose.screens.devices.JadeGenuineCheckScreen
import com.blockstream.compose.screens.exchange.AccountExchangeScreen
import com.blockstream.compose.screens.exchange.BuyScreen
import com.blockstream.compose.screens.exchange.OnOffRampsScreen
import com.blockstream.compose.screens.jade.JadePinUnlockScreen
import com.blockstream.compose.screens.jade.JadeQRScreen
import com.blockstream.compose.screens.lightning.LnUrlAuthScreen
import com.blockstream.compose.screens.lightning.LnUrlWithdrawScreen
import com.blockstream.compose.screens.lightning.RecoverFundsScreen
import com.blockstream.compose.screens.login.LoginScreen
import com.blockstream.compose.screens.onboarding.SetupNewWalletScreen
import com.blockstream.compose.screens.onboarding.hardware.JadeGuideScreen
import com.blockstream.compose.screens.onboarding.hardware.UseHardwareDeviceScreen
import com.blockstream.compose.screens.onboarding.phone.EnterRecoveryPhraseScreen
import com.blockstream.compose.screens.onboarding.phone.PinScreen
import com.blockstream.compose.screens.onboarding.watchonly.WatchOnlyMultisigScreen
import com.blockstream.compose.screens.onboarding.watchonly.WatchOnlySinglesigScreen
import com.blockstream.compose.screens.overview.AccountOverviewScreen
import com.blockstream.compose.screens.overview.SecurityScreen
import com.blockstream.compose.screens.overview.TransactScreen
import com.blockstream.compose.screens.overview.WalletAssetsScreen
import com.blockstream.compose.screens.overview.WalletOverviewScreen
import com.blockstream.compose.screens.promo.PromoScreen
import com.blockstream.compose.screens.receive.ReceiveChooseAccountScreen
import com.blockstream.compose.screens.receive.ReceiveChooseAssetScreen
import com.blockstream.compose.screens.receive.ReceiveScreen
import com.blockstream.compose.screens.recovery.RecoveryCheckScreen
import com.blockstream.compose.screens.recovery.RecoveryIntroScreen
import com.blockstream.compose.screens.recovery.RecoveryPhraseScreen
import com.blockstream.compose.screens.recovery.RecoverySuccessScreen
import com.blockstream.compose.screens.recovery.RecoveryWordsScreen
import com.blockstream.compose.screens.send.BumpScreen
import com.blockstream.compose.screens.send.RedepositScreen
import com.blockstream.compose.screens.send.SendAddressScreen
import com.blockstream.compose.screens.send.SendChooseAccountScreen
import com.blockstream.compose.screens.send.SendChooseAssetScreen
import com.blockstream.compose.screens.send.SendConfirmScreen
import com.blockstream.compose.screens.send.SendScreen
import com.blockstream.compose.screens.send.SweepScreen
import com.blockstream.compose.screens.settings.AppSettingsScreen
import com.blockstream.compose.screens.settings.ChangePinScreen
import com.blockstream.compose.screens.settings.TwoFactorAuthenticationScreen
import com.blockstream.compose.screens.settings.TwoFactorSetupScreen
import com.blockstream.compose.screens.settings.WalletSettingsScreen
import com.blockstream.compose.screens.settings.WatchOnlyScreen
import com.blockstream.compose.screens.support.SupportScreen
import com.blockstream.compose.screens.swap.SwapScreen
import com.blockstream.compose.screens.transaction.TransactionScreen
import com.blockstream.compose.screens.twofactor.ReEnable2FAScreen
import com.blockstream.compose.sheets.AccountRenameBottomSheet
import com.blockstream.compose.sheets.AccountsBottomSheet
import com.blockstream.compose.sheets.AnalyticsBottomSheet
import com.blockstream.compose.sheets.AskJadeUnlockBottomSheet
import com.blockstream.compose.sheets.AssetDetailsBottomSheet
import com.blockstream.compose.sheets.AssetsAccountsBottomSheet
import com.blockstream.compose.sheets.AssetsBottomSheet
import com.blockstream.compose.sheets.Bip39PassphraseBottomSheet
import com.blockstream.compose.sheets.BuyQuotesBottomSheet
import com.blockstream.compose.sheets.Call2ActionBottomSheet
import com.blockstream.compose.sheets.CameraBottomSheet
import com.blockstream.compose.sheets.ChooseAssetAccountBottomSheet
import com.blockstream.compose.sheets.CountriesBottomSheet
import com.blockstream.compose.sheets.DenominationBottomSheet
import com.blockstream.compose.sheets.DeviceInteractionBottomSheet
import com.blockstream.compose.sheets.EnableJadeFeatureBottomSheet
import com.blockstream.compose.sheets.EnvironmentBottomSheet
import com.blockstream.compose.dialogs.HwWatchOnlyDialog
import com.blockstream.compose.sheets.FeeRateBottomSheet
import com.blockstream.compose.sheets.JadeFirmwareUpdateBottomSheet
import com.blockstream.compose.sheets.LightningNodeBottomSheet
import com.blockstream.compose.sheets.MainMenuBottomSheet
import com.blockstream.compose.sheets.MeldCountriesBottomSheet
import com.blockstream.compose.sheets.MenuBottomSheetView
import com.blockstream.compose.sheets.NewJadeConnectedBottomSheet
import com.blockstream.compose.sheets.NoteBottomSheet
import com.blockstream.compose.sheets.PassphraseBottomSheet
import com.blockstream.compose.sheets.PinMatrixBottomSheet
import com.blockstream.compose.sheets.QrBottomSheet
import com.blockstream.compose.sheets.RecoveryHelpBottomSheet
import com.blockstream.compose.sheets.SecurityLevelBottomSheet
import com.blockstream.compose.sheets.SignMessageBottomSheet
import com.blockstream.compose.sheets.SwapFeesBottomSheet
import com.blockstream.compose.sheets.SystemMessageBottomSheet
import com.blockstream.compose.sheets.TransactionDetailsBottomSheet
import com.blockstream.compose.sheets.TwoFactorResetBottomSheet
import com.blockstream.compose.sheets.WalletDeleteBottomSheet
import com.blockstream.compose.sheets.WalletRenameBottomSheet
import com.blockstream.compose.sheets.WatchOnlyCredentialsSettingsBottomSheet
import com.blockstream.compose.utils.StringHolder
import com.blockstream.data.devices.DeviceModel
import com.blockstream.data.managers.DeviceManager
import org.koin.compose.koinInject

@Composable
fun Router(
    mainViewModel: MainViewModel,
    innerPadding: PaddingValues,
    navController: NavHostController,
    startDestination: NavigateDestination = NavigateDestinations.Home,
) {

    CompositionLocalProvider(
        LocalInnerPadding provides innerPadding
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = {
                fadeIn() + slideInHorizontally(initialOffsetX = {
                    it / 2
                })
            },
            exitTransition = {
                fadeOut() + slideOutHorizontally()
            },
            popEnterTransition = {
                fadeIn() + slideInHorizontally()
            },
            popExitTransition = {
                fadeOut() + slideOutHorizontally(targetOffsetX = {
                    it
                })
            }
        ) {
            appComposable<NavigateDestinations.Home> {
                HomeScreen(viewModel { HomeViewModel() })
            }
            appComposable<NavigateDestinations.GetStarted> {
                HomeScreen(viewModel { HomeViewModel(isGetStarted = true) })
            }
            appComposable<NavigateDestinations.About> {
                AboutScreen(viewModel { AboutViewModel() })
            }
            appComposable<NavigateDestinations.AppSettings> {
                AppSettingsScreen(viewModel { AppSettingsViewModel() })
            }
            appComposable<NavigateDestinations.SetupNewWallet> {
                SetupNewWalletScreen(viewModel { SetupNewWalletViewModel() })
            }
            appComposable<NavigateDestinations.ArchivedAccounts> {
                val args = it.toRoute<NavigateDestinations.ArchivedAccounts>()
                ArchivedAccountsScreen(viewModel {
                    ArchivedAccountsViewModel(
                        greenWallet = args.greenWallet,
                        navigateToRoot = args.navigateToRoot
                    )
                })
            }
            appComposable<NavigateDestinations.WatchOnlySinglesig> {
                val args = it.toRoute<NavigateDestinations.WatchOnlySinglesig>()
                WatchOnlySinglesigScreen(viewModel { WatchOnlySinglesigViewModel(setupArgs = args.setupArgs) })
            }
            appComposable<NavigateDestinations.WatchOnlyMultisig> {
                val args = it.toRoute<NavigateDestinations.WatchOnlyMultisig>()
                WatchOnlyMultisigScreen(viewModel { WatchOnlyMultisigViewModel(setupArgs = args.setupArgs) })
            }
            appComposable<NavigateDestinations.RecoveryIntro> {
                val stateKeeperFactory = rememberStateKeeperFactory()
                val args = it.toRoute<NavigateDestinations.RecoveryIntro>()
                RecoveryIntroScreen(viewModel {
                    RecoveryIntroViewModel(
                        setupArgs = args.setupArgs,
                        stateKeeper = stateKeeperFactory.stateKeeper()
                    )
                })
            }
            appComposable<NavigateDestinations.RecoveryWords> {
                val args = it.toRoute<NavigateDestinations.RecoveryWords>()
                RecoveryWordsScreen(viewModel { RecoveryWordsViewModel(setupArgs = args.setupArgs) })
            }
            appComposable<NavigateDestinations.RecoveryCheck> {
                val args = it.toRoute<NavigateDestinations.RecoveryCheck>()
                RecoveryCheckScreen(viewModel { RecoveryCheckViewModel(setupArgs = args.setupArgs) })
            }
            appComposable<NavigateDestinations.RecoveryPhrase> {
                val args = it.toRoute<NavigateDestinations.RecoveryPhrase>()
                RecoveryPhraseScreen(viewModel {
                    RecoveryPhraseViewModel(
                        isLightningDerived = args.setupArgs.isLightningDerived,
                        providedCredentials = args.setupArgs.credentials,
                        greenWallet = args.setupArgs.greenWallet
                    )
                })
            }
            appComposable<NavigateDestinations.RecoverySuccess> {
                val args = it.toRoute<NavigateDestinations.RecoverySuccess>()
                RecoverySuccessScreen(
                    viewModel {
                        RecoverySuccessViewModel(greenWallet = args.greenWallet)
                    }
                )
            }
            appComposable<NavigateDestinations.SetPin> {
                val args = it.toRoute<NavigateDestinations.SetPin>()
                PinScreen(viewModel { PinViewModel(setupArgs = args.setupArgs) })
            }
            appComposable<NavigateDestinations.EnterRecoveryPhrase> {
                val stateKeeperFactory = rememberStateKeeperFactory()
                val args = it.toRoute<NavigateDestinations.EnterRecoveryPhrase>()
                EnterRecoveryPhraseScreen(viewModel {
                    EnterRecoveryPhraseViewModel(
                        setupArgs = args.setupArgs,
                        stateKeeper = stateKeeperFactory.stateKeeper()
                    )
                })
            }
            appComposable<NavigateDestinations.UseHardwareDevice> {
                UseHardwareDeviceScreen(viewModel { UseHardwareDeviceViewModel() })
            }
            appComposable<NavigateDestinations.DeviceList> {
                val args = it.toRoute<NavigateDestinations.DeviceList>()
                DeviceListScreen(viewModel { DeviceListViewModel(args.isJade) })
            }
            appComposable<NavigateDestinations.DeviceInfo> {
                val args = it.toRoute<NavigateDestinations.DeviceInfo>()
                DeviceInfoScreen(viewModel { DeviceInfoViewModel(args.deviceId) })
            }
            appComposable<NavigateDestinations.Login> {
                val args = it.toRoute<NavigateDestinations.Login>()
                LoginScreen(viewModel {
                    LoginViewModel(
                        greenWallet = args.greenWallet,
                        deviceId = args.deviceId,
                        autoLoginWallet = args.autoLoginWallet,
                        isWatchOnlyUpgrade = args.isWatchOnlyUpgrade,
                        isNewWallet = args.isNewWallet
                    )
                })
            }
            appComposable<NavigateDestinations.WalletOverview> {
                val args = it.toRoute<NavigateDestinations.WalletOverview>()
                WalletOverviewScreen(viewModel {
                    WalletOverviewViewModel(
                        greenWallet = args.greenWallet,
                        showWalletOnboarding = args.showWalletOnboarding
                    )
                })
            }
            appComposable<NavigateDestinations.Transact> {
                val args = it.toRoute<NavigateDestinations.Transact>()
                val viewModel = viewModel {
                    TransactViewModel(
                        greenWallet = args.greenWallet
                    )
                }
                TransactScreen(viewModel = viewModel)
            }
            appComposable<NavigateDestinations.Security> {
                val args = it.toRoute<NavigateDestinations.Security>()
                SecurityScreen(viewModel {
                    SecurityViewModel(
                        greenWallet = args.greenWallet
                    )
                })
            }
            appComposable<NavigateDestinations.AccountOverview> {
                val args = it.toRoute<NavigateDestinations.AccountOverview>()
                AccountOverviewScreen(viewModel {
                    AccountOverviewViewModel(
                        greenWallet = args.greenWallet,
                        accountAsset = args.accountAsset
                    )
                })
            }
            appComposable<NavigateDestinations.Transaction> {
                val args = it.toRoute<NavigateDestinations.Transaction>()
                TransactionScreen(viewModel {
                    TransactionViewModel(
                        greenWallet = args.greenWallet,
                        transaction = args.transaction
                    )
                })
            }
            appComposable<NavigateDestinations.ChooseAccountType> {
                val args = it.toRoute<NavigateDestinations.ChooseAccountType>()
                ChooseAccountTypeScreen(viewModel {
                    ChooseAccountTypeViewModel(
                        greenWallet = args.greenWallet,
                        initAsset = args.assetBalance,
                        allowAssetSelection = args.allowAssetSelection,
                        popTo = args.popTo
                    )
                })
            }
            appComposable<NavigateDestinations.ReviewAddAccount> {
                val args = it.toRoute<NavigateDestinations.ReviewAddAccount>()
                ReviewAddAccountScreen(viewModel {
                    ReviewAddAccountViewModel(
                        setupArgs = args.setupArgs
                    )
                })
            }
            appComposable<NavigateDestinations.Xpub> {
                val args = it.toRoute<NavigateDestinations.Xpub>()
                XpubScreen(viewModel {
                    XpubViewModel(
                        setupArgs = args.setupArgs,
                    )
                })
            }
            appComposable<NavigateDestinations.AddAccount2of3> {
                val args = it.toRoute<NavigateDestinations.AddAccount2of3>()
                Account2of3Screen(viewModel {
                    Account2of3ViewModel(
                        setupArgs = args.setupArgs,
                    )
                })
            }
            appComposable<NavigateDestinations.WalletSettings> {
                val args = it.toRoute<NavigateDestinations.WalletSettings>()
                WalletSettingsScreen(viewModel {
                    WalletSettingsViewModel(
                        greenWallet = args.greenWallet,
                        section = args.section,
                        network = args.network
                    )
                })
            }
            appComposable<NavigateDestinations.ChangePin> {
                val args = it.toRoute<NavigateDestinations.ChangePin>()
                ChangePinScreen(viewModel {
                    WalletSettingsViewModel(
                        greenWallet = args.greenWallet,
                        section = WalletSettingsSection.ChangePin,
                        network = null,
                        isRecoveryConfirmation = args.isRecoveryConfirmation
                    )
                })
            }
            appComposable<NavigateDestinations.WatchOnly> {
                val args = it.toRoute<NavigateDestinations.WatchOnly>()
                WatchOnlyScreen(viewModel {
                    WatchOnlyViewModel(
                        greenWallet = args.greenWallet
                    )
                })
            }
            appComposable<NavigateDestinations.Receive> {
                val args = it.toRoute<NavigateDestinations.Receive>()
                ReceiveScreen(viewModel {
                    ReceiveViewModel(
                        greenWallet = args.greenWallet,
                        accountAsset = args.accountAsset
                    )
                })
            }
            appComposable<NavigateDestinations.ReceiveChooseAsset> {
                val args = it.toRoute<NavigateDestinations.ReceiveChooseAsset>()
                ReceiveChooseAssetScreen(viewModel {
                    ReceiveChooseAssetViewModel(
                        greenWallet = args.greenWallet
                    )
                })
            }
            appComposable<NavigateDestinations.ReceiveChooseAccount> {
                val args = it.toRoute<NavigateDestinations.ReceiveChooseAccount>()
                ReceiveChooseAccountScreen(viewModel {
                    ReceiveChooseAccountViewModel(
                        greenWallet = args.greenWallet,
                        accounts = args.accounts.list,
                    )
                })
            }
            appComposable<NavigateDestinations.SendChooseAsset> {
                val args = it.toRoute<NavigateDestinations.SendChooseAsset>()
                SendChooseAssetScreen(viewModel {
                    SendChooseAssetViewModel(
                        greenWallet = args.greenWallet,
                        address = args.address,
                        addressType = args.addressType,
                        assets = args.assets.list
                    )
                })
            }
            appComposable<NavigateDestinations.SendChooseAccount> {
                val args = it.toRoute<NavigateDestinations.SendChooseAccount>()
                SendChooseAccountScreen(viewModel {
                    SendChooseAccountViewModel(
                        greenWallet = args.greenWallet,
                        address = args.address,
                        addressType = args.addressType,
                        asset = args.asset,
                        accounts = args.accounts.list,
                        // initialAccountAsset = args.accounts.list.firstOrNull()?.accountAsset
                    )
                })
            }
            appComposable<NavigateDestinations.Send> {
                val args = it.toRoute<NavigateDestinations.Send>()
                SendScreen(viewModel {
                    SendViewModel(
                        greenWallet = args.greenWallet,
                        address = args.address,
                        addressType = args.addressType,
                        accountAsset = args.accountAsset
                    )
                })
            }
            appComposable<NavigateDestinations.SendAddress> {
                val args = it.toRoute<NavigateDestinations.SendAddress>()
                SendAddressScreen(viewModel {
                    SendAddressViewModel(
                        greenWallet = args.greenWallet,
                        initAddress = args.address,
                        addressType = args.addressType,
                        initialAccountAsset = args.accountAsset
                    )
                })
            }
            appComposable<NavigateDestinations.SendConfirm> {
                val args = it.toRoute<NavigateDestinations.SendConfirm>()
                SendConfirmScreen(viewModel {
                    SendConfirmViewModel(
                        greenWallet = args.greenWallet,
                        accountAsset = args.accountAsset,
                        denomination = args.denomination
                    )
                })
            }
            appComposable<NavigateDestinations.AccountExchange> {
                val args = it.toRoute<NavigateDestinations.AccountExchange>()
                AccountExchangeScreen(viewModel {
                    AccountExchangeViewModel(
                        greenWallet = args.greenWallet
                    )
                })
            }
            appComposable<NavigateDestinations.Swap> {
                val args = it.toRoute<NavigateDestinations.Swap>()
                SwapScreen(viewModel {
                    SwapViewModel(
                        greenWallet = args.greenWallet,
                        accountAssetOrNull = args.accountAsset
                    )
                })
            }
            appComposable<NavigateDestinations.Buy> {
                val args = it.toRoute<NavigateDestinations.Buy>()
                BuyScreen(viewModel {
                    BuyViewModel(
                        greenWallet = args.greenWallet,
                        initialAccountAsset = args.accountAsset
                    )
                })
            }
            appComposable<NavigateDestinations.OnOffRamps> {
                val args = it.toRoute<NavigateDestinations.OnOffRamps>()
                OnOffRampsScreen(viewModel {
                    OnOffRampsViewModel(
                        greenWallet = args.greenWallet
                    )
                })
            }
            appComposable<NavigateDestinations.Addresses> {
                val args = it.toRoute<NavigateDestinations.Addresses>()
                AddressesScreen(viewModel {
                    AddressesViewModel(
                        greenWallet = args.greenWallet,
                        accountAsset = args.accountAsset
                    )
                })
            }
            appComposable<NavigateDestinations.ArchivedAccounts> {
                val args = it.toRoute<NavigateDestinations.ArchivedAccounts>()
                ArchivedAccountsScreen(viewModel {
                    ArchivedAccountsViewModel(
                        greenWallet = args.greenWallet,
                        navigateToRoot = args.navigateToRoot
                    )
                })
            }
            appComposable<NavigateDestinations.LnUrlAuth> {
                val args = it.toRoute<NavigateDestinations.LnUrlAuth>()
                LnUrlAuthScreen(viewModel {
                    LnUrlAuthViewModel(
                        greenWallet = args.greenWallet,
                        requestData = args.lnUrlAuthRequest.deserialize()
                    )
                })
            }
            appComposable<NavigateDestinations.LnUrlWithdraw> {
                val args = it.toRoute<NavigateDestinations.LnUrlWithdraw>()
                LnUrlWithdrawScreen(viewModel {
                    LnUrlWithdrawViewModel(
                        greenWallet = args.greenWallet,
                        requestData = args.lnUrlWithdrawRequest.deserialize()
                    )
                })
            }
            appComposable<NavigateDestinations.RecoverFunds> {
                val args = it.toRoute<NavigateDestinations.RecoverFunds>()
                RecoverFundsScreen(viewModel {
                    RecoverFundsViewModel(
                        greenWallet = args.greenWallet,
                        isSendAll = args.isSendAll,
                        onChainAddress = args.address,
                        satoshi = args.amount
                    )
                })
            }
            appComposable<NavigateDestinations.JadeQR> {
                val args = it.toRoute<NavigateDestinations.JadeQR>()
                JadeQRScreen(viewModel {
                    JadeQRViewModel(
                        greenWalletOrNull = args.greenWalletOrNull,
                        operation = args.operation,
                        deviceModel = args.deviceModel ?: DeviceModel.Generic
                    )
                })
            }
            appComposable<NavigateDestinations.ImportPubKey> {
                val args = it.toRoute<NavigateDestinations.ImportPubKey>()
                ImportPubKeyScreen(viewModel {
                    ImportPubKeyViewModel(
                        deviceModel = args.deviceModel
                    )
                })
            }
            appComposable<NavigateDestinations.Sweep> {
                val args = it.toRoute<NavigateDestinations.Sweep>()
                SweepScreen(viewModel {
                    SweepViewModel(
                        greenWallet = args.greenWallet,
                        privateKey = args.privateKey,
                        accountAssetOrNull = args.accountAsset
                    )
                })
            }
            appComposable<NavigateDestinations.Bump> {
                val args = it.toRoute<NavigateDestinations.Bump>()
                BumpScreen(viewModel {
                    BumpViewModel(
                        greenWallet = args.greenWallet,
                        accountAsset = args.accountAsset,
                        transactionAsString = args.transaction
                    )
                })
            }
            appComposable<NavigateDestinations.Redeposit> {
                val args = it.toRoute<NavigateDestinations.Redeposit>()
                RedepositScreen(viewModel {
                    RedepositViewModel(
                        greenWallet = args.greenWallet,
                        accountAsset = args.accountAsset,
                        isRedeposit2FA = args.isRedeposit2FA
                    )
                })
            }
            appComposable<NavigateDestinations.ReEnable2FA> {
                val args = it.toRoute<NavigateDestinations.ReEnable2FA>()
                ReEnable2FAScreen(viewModel {
                    ReEnable2FAViewModel(
                        greenWallet = args.greenWallet
                    )
                })
            }
            appComposable<NavigateDestinations.WalletAssets> {
                val args = it.toRoute<NavigateDestinations.WalletAssets>()
                WalletAssetsScreen(viewModel {
                    WalletAssetsViewModel(greenWallet = args.greenWallet)
                })
            }
            appComposable<NavigateDestinations.JadePinUnlock> {
                JadePinUnlockScreen(viewModel {
                    SimpleGreenViewModel(
                        screenName = "JadePinUnlock"
                    )
                })
            }
            appComposable<NavigateDestinations.Promo> {
                val args = it.toRoute<NavigateDestinations.Promo>()
                PromoScreen(
                    viewModel = viewModel {
                        PromoViewModel(
                            args.promo,
                            args.greenWalletOrNull
                        )
                    })
            }
            appComposable<NavigateDestinations.Support> {
                val args = it.toRoute<NavigateDestinations.Support>()
                SupportScreen(viewModel {
                    SupportViewModel(
                        type = args.type,
                        supportData = args.supportData,
                        greenWalletOrNull = args.greenWalletOrNull
                    )
                })
            }

            appComposable<NavigateDestinations.TwoFactorSetup> {
                val args = it.toRoute<NavigateDestinations.TwoFactorSetup>()
                TwoFactorSetupScreen(viewModel {
                    TwoFactorSetupViewModel(
                        greenWallet = args.greenWallet,
                        network = args.network,
                        method = args.method,
                        action = args.action,
                        isSmsBackup = args.isSmsBackup
                    )
                })
            }
            appComposable<NavigateDestinations.DeviceList> {
                val args = it.toRoute<NavigateDestinations.DeviceList>()
                DeviceListScreen(viewModel {
                    DeviceListViewModel(
                        isJade = args.isJade
                    )
                })
            }
            appComposable<NavigateDestinations.DeviceInfo> {
                val args = it.toRoute<NavigateDestinations.DeviceInfo>()
                DeviceInfoScreen(viewModel {
                    DeviceInfoViewModel(
                        deviceId = args.deviceId
                    )
                })
            }
            appComposable<NavigateDestinations.JadeGenuineCheck> {
                val args = it.toRoute<NavigateDestinations.JadeGenuineCheck>()
                JadeGenuineCheckScreen(viewModel {
                    JadeGenuineCheckViewModel(
                        greenWalletOrNull = args.greenWalletOrNull,
                        deviceId = args.deviceId
                    )
                })
            }
            appComposable<NavigateDestinations.TwoFactorAuthentication> {
                val args = it.toRoute<NavigateDestinations.TwoFactorAuthentication>()

                val viewModel = viewModel {
                    TwoFactorAuthenticationViewModel(
                        greenWallet = args.greenWallet
                    )
                }

                val networkViewModels by remember {
                    mutableStateOf(
                        viewModel.networks.map {
                            WalletSettingsViewModel(
                                greenWallet = args.greenWallet,
                                section = WalletSettingsSection.TwoFactor,
                                network = it
                            )
                        }
                    )
                }

                TwoFactorAuthenticationScreen(
                    viewModel = viewModel,
                    networkViewModels = networkViewModels,
                    network = args.network
                )
            }
            appComposable<NavigateDestinations.JadeGuide> {
                JadeGuideScreen(
                    viewModel = viewModel { JadeGuideViewModel() }
                )
            }
            appComposable<NavigateDestinations.DeviceScan> {
                val args = it.toRoute<NavigateDestinations.DeviceScan>()
                DeviceScanScreen(viewModel {
                    DeviceScanViewModel(
                        greenWallet = args.greenWallet,
                        isWatchOnlyUpgrade = args.isWatchOnlyUpgrade,
                        isWatchOnlyDeviceConnect = args.isWatchOnlyDeviceConnect
                    )
                })
            }
            // Bottom sheets
            appBottomSheet<NavigateDestinations.Analytics> {
                val args = it.toRoute<NavigateDestinations.Analytics>()
                AnalyticsBottomSheet(
                    viewModel = viewModel { AnalyticsViewModel(isActionRequired = args.isActionRequired) },
                    onDismissRequest = navController.onDismissRequest {
                        args.setResult(true)
                    }
                )
            }
            appBottomSheet<NavigateDestinations.RenameAccount> {
                val args = it.toRoute<NavigateDestinations.RenameAccount>()
                AccountRenameBottomSheet(
                    viewModel = viewModel {
                        SimpleGreenViewModel(
                            greenWalletOrNull = args.greenWallet,
                            accountAssetOrNull = args.account.accountAsset,
                            screenName = "RenameAccount"
                        )
                    },
                    onDismissRequest = navController.onDismissRequest()
                )
            }
            appBottomSheet<NavigateDestinations.Environment> {
                EnvironmentBottomSheet(onDismissRequest = navController.onDismissRequest())
            }
            appBottomSheet<NavigateDestinations.DeleteWallet> {
                val args = it.toRoute<NavigateDestinations.DeleteWallet>()
                WalletDeleteBottomSheet(
                    viewModel = viewModel { WalletDeleteViewModel(greenWallet = args.greenWallet) },
                    onDismissRequest = navController.onDismissRequest()
                )
            }
            appBottomSheet<NavigateDestinations.RenameWallet> {
                val args = it.toRoute<NavigateDestinations.RenameWallet>()
                WalletRenameBottomSheet(
                    viewModel = viewModel { WalletNameViewModel(greenWallet = args.greenWallet) },
                    onDismissRequest = navController.onDismissRequest()
                )
            }
            appBottomSheet<NavigateDestinations.Countries> {
                val args = it.toRoute<NavigateDestinations.Countries>()
                CountriesBottomSheet(
                    viewModel = viewModel {
                        SimpleGreenViewModel(
                            greenWalletOrNull = args.greenWallet,
                            screenName = "Countries"
                        )
                    },
                    title = args.title,
                    subtitle = args.subtitle,
                    showDialCode = args.showDialCode,
                    onDismissRequest = navController.onDismissRequest()
                )
            }
            appBottomSheet<NavigateDestinations.MeldCountries> {
                val args = it.toRoute<NavigateDestinations.MeldCountries>()
                MeldCountriesBottomSheet(
                    viewModel = viewModel {
                        MeldCountriesViewModel(
                            greenWallet = args.greenWallet
                        )
                    },
                    title = args.title,
                    subtitle = args.subtitle,
                    onDismissRequest = navController.onDismissRequest()
                )
            }
            appBottomSheet<NavigateDestinations.Note> {
                val args = it.toRoute<NavigateDestinations.Note>()
                NoteBottomSheet(
                    viewModel = viewModel {
                        NoteViewModel(
                            initialNote = args.note,
                            noteType = args.noteType,
                            greenWallet = args.greenWallet
                        )
                    },
                    onDismissRequest = navController.onDismissRequest()
                )
            }
            appBottomSheet<NavigateDestinations.Denomination> {
                val args = it.toRoute<NavigateDestinations.Denomination>()
                DenominationBottomSheet(
                    viewModel = viewModel {
                        DenominationViewModel(
                            greenWallet = args.greenWallet,
                            denominatedValue = args.denominatedValue
                        )
                    },
                    onDismissRequest = navController.onDismissRequest()
                )
            }
            appBottomSheet<NavigateDestinations.ChooseAssetAccounts> {
                val args = it.toRoute<NavigateDestinations.ChooseAssetAccounts>()
                ChooseAssetAccountBottomSheet(
                    viewModel = viewModel {
                        SimpleGreenViewModel(
                            greenWalletOrNull = args.greenWallet,
                            screenName = "ChooseAssetAndAccount"
                        )
                    },
                    onDismissRequest = navController.onDismissRequest()
                )
            }
            appBottomSheet<NavigateDestinations.Camera> {
                val args = it.toRoute<NavigateDestinations.Camera>()
                CameraBottomSheet(
                    viewModel = viewModel {
                        CameraViewModel(
                            isDecodeContinuous = args.isDecodeContinuous,
                            parentScreenName = args.parentScreenName,
                            setupArgs = args.setupArgs
                        )
                    },
                    onDismissRequest = navController.onDismissRequest()
                )
            }
            appBottomSheet<NavigateDestinations.Bip39Passphrase> {
                val args = it.toRoute<NavigateDestinations.Bip39Passphrase>()
                Bip39PassphraseBottomSheet(
                    viewModel = viewModel {
                        Bip39PassphraseViewModel(
                            greenWallet = args.greenWallet,
                            passphrase = args.passphrase
                        )
                    },
                    onDismissRequest = navController.onDismissRequest()
                )
            }
            appBottomSheet<NavigateDestinations.AssetDetails> {
                val args = it.toRoute<NavigateDestinations.AssetDetails>()
                AssetDetailsBottomSheet(
                    viewModel = viewModel {
                        AssetDetailsViewModel(
                            greenWallet = args.greenWallet,
                            assetId = args.assetId,
                            accountAsset = args.accountAsset
                        )
                    },
                    onDismissRequest = navController.onDismissRequest()
                )
            }
            appBottomSheet<NavigateDestinations.TransactionDetails> {
                val args = it.toRoute<NavigateDestinations.TransactionDetails>()
                TransactionDetailsBottomSheet(
                    viewModel = viewModel {
                        TransactionDetailsViewModel(
                            greenWallet = args.greenWallet,
                            initialTransaction = args.transaction
                        )
                    },
                    onDismissRequest = navController.onDismissRequest()
                )
            }
            appComposable<NavigateDestinations.AssetAccountList> {
                val args = it.toRoute<NavigateDestinations.AssetAccountList>()
                AssetAccountListScreen(
                    viewModel = viewModel {
                        AssetAccountListViewModel(
                            greenWallet = args.greenWallet,
                            assetId = args.assetId
                        )
                    }
                )
            }
            appComposable<NavigateDestinations.AssetAccountDetails> {
                val args = it.toRoute<NavigateDestinations.AssetAccountDetails>()
                AssetAccountDetailsScreen(
                    viewModel = viewModel {
                        AssetAccountDetailsViewModel(
                            greenWallet = args.greenWallet,
                            accountAsset = args.accountAsset
                        )
                    }
                )
            }
            appComposable<NavigateDestinations.AccountDescriptor> {
                val args = it.toRoute<NavigateDestinations.AccountDescriptor>()
                AccountDescriptorScreen(
                    viewModel = viewModel {
                        AccountDescriptorViewModel(
                            greenWallet = args.greenWallet,
                            accountAsset = args.accountAsset
                        )
                    }
                )
            }
            appBottomSheet<NavigateDestinations.JadeFirmwareUpdate> {
                val args = it.toRoute<NavigateDestinations.JadeFirmwareUpdate>()
                JadeFirmwareUpdateBottomSheet(
                    viewModel = viewModel {
                        JadeFirmwareUpdateViewModel(
                            deviceId = args.deviceId
                        )
                    },
                    onDismissRequest = navController.onDismissRequest()
                )
            }
            appBottomSheet<NavigateDestinations.NewJadeConnected> {
                NewJadeConnectedBottomSheet(
                    viewModel = viewModel {
                        SimpleGreenViewModel(
                            screenName = "NewJadeConnected"
                        )
                    },
                    onDismissRequest = navController.onDismissRequest()
                )
            }
            appDialog<NavigateDestinations.HwWatchOnlyChoice>(
                dialogProperties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                )
            ) {
                HwWatchOnlyDialog(
                    onDismissRequest = {
                        navController.popBackStack()
                    }
                )
            }
            appBottomSheet<NavigateDestinations.AskJadeUnlock> {
                val args = it.toRoute<NavigateDestinations.AskJadeUnlock>()
                AskJadeUnlockBottomSheet(
                    viewModel = viewModel { SimpleGreenViewModel(screenName = "AskJadeUnlock") },
                    isOnboarding = args.isOnboarding,
                    onDismissRequest = navController.onDismissRequest()
                )
            }
            appBottomSheet<NavigateDestinations.DeviceInteraction> {
                val args = it.toRoute<NavigateDestinations.DeviceInteraction>()

                val deviceManager = koinInject<DeviceManager>()

                val screenName = when {
                    args.verifyAddress != null -> "VerifyAddress"
                    args.transactionConfirmation != null -> "VerifyTransaction"
                    else -> null
                }

                val viewModel = viewModel {
                    SimpleGreenViewModel(
                        greenWalletOrNull = args.greenWalletOrNull,
                        screenName = screenName,
                        device = deviceManager.getDevice(args.deviceId)
                    )
                }

                DeviceInteractionBottomSheet(
                    viewModel = viewModel,
                    transactionConfirmation = args.transactionConfirmation,
                    verifyAddress = args.verifyAddress,
                    isMasterBlindingKeyRequest = args.isMasterBlindingKeyRequest,
                    message = StringHolder.create(args.message),
                    onDismissRequest = navController.onDismissRequest()
                )
            }
            appBottomSheet<NavigateDestinations.TwoFactorReset> {
                val args = it.toRoute<NavigateDestinations.TwoFactorReset>()
                TwoFactorResetBottomSheet(
                    viewModel = viewModel {
                        SimpleGreenViewModel(
                            greenWalletOrNull = args.greenWallet,
                            screenName = "TwoFactorReset"
                        )
                    },
                    network = args.network,
                    twoFactorReset = args.twoFactorReset,
                    onDismissRequest = navController.onDismissRequest()
                )
            }
            appBottomSheet<NavigateDestinations.SystemMessage> {
                val args = it.toRoute<NavigateDestinations.SystemMessage>()
                SystemMessageBottomSheet(
                    viewModel = viewModel {
                        SimpleGreenViewModel(
                            greenWalletOrNull = args.greenWallet,
                            screenName = "SystemMessage"
                        )
                    },
                    network = args.network,
                    message = args.message,
                    onDismissRequest = navController.onDismissRequest()
                )
            }
            appBottomSheet<NavigateDestinations.Qr> {
                val args = it.toRoute<NavigateDestinations.Qr>()
                QrBottomSheet(
                    viewModel = viewModel {
                        SimpleGreenViewModel(
                            greenWalletOrNull = args.greenWallet,
                        )
                    },
                    title = args.title,
                    subtitle = args.subtitle,
                    data = args.data,
                    onDismissRequest = navController.onDismissRequest()
                )
            }
            appBottomSheet<NavigateDestinations.AssetsAccounts> {
                val args = it.toRoute<NavigateDestinations.AssetsAccounts>()
                AssetsAccountsBottomSheet(
                    viewModel = viewModel {
                        SimpleGreenViewModel(
                            greenWalletOrNull = args.greenWallet,
                        )
                    },
                    assetsAccounts = args.assetsAccounts,
                    onDismissRequest = navController.onDismissRequest()
                )
            }
            appBottomSheet<NavigateDestinations.EnableTwoFactor> {
                val args = it.toRoute<NavigateDestinations.EnableTwoFactor>()
                Call2ActionBottomSheet(
                    viewModel = viewModel {
                        SimpleGreenViewModel(
                            greenWalletOrNull = args.greenWallet,
                        )
                    },
                    network = args.network,
                    onDismissRequest = navController.onDismissRequest()
                )
            }
            appBottomSheet<NavigateDestinations.Accounts> {
                val args = it.toRoute<NavigateDestinations.Accounts>()
                AccountsBottomSheet(
                    viewModel = viewModel {
                        SimpleGreenViewModel(
                            greenWalletOrNull = args.greenWallet,
                        )
                    },
                    accountsBalance = args.accounts,
                    title = args.title,
                    message = args.message,
                    withAsset = args.withAsset,
                    withAssetIcon = args.withAssetIcon,
                    withArrow = args.withArrow,
                    withAction = args.withAction,
                    onDismissRequest = navController.onDismissRequest()
                )
            }
            appBottomSheet<NavigateDestinations.SecurityLevel> {
                val args = it.toRoute<NavigateDestinations.SecurityLevel>()
                SecurityLevelBottomSheet(
                    viewModel = viewModel {
                        SetupNewWalletViewModel(
                            greenWalletOrNull = args.greenWallet,
                        )
                    },
                    onDismissRequest = navController.onDismissRequest()
                )
            }
            appBottomSheet<NavigateDestinations.SignMessage> {
                val args = it.toRoute<NavigateDestinations.SignMessage>()
                SignMessageBottomSheet(
                    viewModel = viewModel {
                        SignMessageViewModel(
                            greenWallet = args.greenWallet,
                            accountAsset = args.accountAsset,
                            address = args.address
                        )
                    },
                    onDismissRequest = navController.onDismissRequest()
                )
            }
            appBottomSheet<NavigateDestinations.LightningNode> {
                val args = it.toRoute<NavigateDestinations.LightningNode>()
                LightningNodeBottomSheet(
                    viewModel = viewModel {
                        LightningNodeViewModel(
                            greenWallet = args.greenWallet
                        )
                    },
                    onDismissRequest = navController.onDismissRequest()
                )
            }
            appBottomSheet<NavigateDestinations.SwapFees> {
                val args = it.toRoute<NavigateDestinations.SwapFees>()
                SwapFeesBottomSheet(
                    networkFee = args.networkFee,
                    swapFee = args.swapFee,
                    totalFees = args.totalFees,
                    totalFeesFiat = args.totalFeesFiat,
                    onDismissRequest = navController.onDismissRequest()
                )
            }
            appBottomSheet<NavigateDestinations.WatchOnlyCredentialsSettings> {
                val args = it.toRoute<NavigateDestinations.WatchOnlyCredentialsSettings>()
                WatchOnlyCredentialsSettingsBottomSheet(
                    viewModel = viewModel {
                        WatchOnlyCredentialsSettingsViewModel(
                            greenWallet = args.greenWallet,
                            network = args.network
                        )
                    },
                    onDismissRequest = navController.onDismissRequest()
                )
            }
            appBottomSheet<NavigateDestinations.Assets> {
                val args = it.toRoute<NavigateDestinations.Assets>()

                AssetsBottomSheet(
                    viewModel = viewModel {
                        SimpleGreenViewModel(
                            greenWalletOrNull = args.greenWallet,
                        )
                    },
                    assetBalance = args.assets,
                    onDismissRequest = navController.onDismissRequest()
                )
            }
            appBottomSheet<NavigateDestinations.RecoveryHelp> {
                RecoveryHelpBottomSheet(
                    viewModel = viewModel {
                        RecoveryHelpViewModel()
                    },
                    onDismissRequest = navController.onDismissRequest()
                )
            }
            appBottomSheet<NavigateDestinations.Menu> {
                val args = it.toRoute<NavigateDestinations.Menu>()
                MenuBottomSheetView(
                    title = args.title,
                    subtitle = args.subtitle,
                    entries = args.entries,
                    onSelect = { position, menuEntry ->
                        NavigateDestinations.Menu.setResult(position)
                    },
                    onDismissRequest = navController.onDismissRequest()
                )
            }
            appBottomSheet<NavigateDestinations.MainMenu> {
                val args = it.toRoute<NavigateDestinations.MainMenu>()
                MainMenuBottomSheet(
                    isTestnet = args.isTestnet,
                    onDismissRequest = navController.onDismissRequest()
                )
            }
            appBottomSheet<NavigateDestinations.FeeRate> {
                val args = it.toRoute<NavigateDestinations.FeeRate>()
                FeeRateBottomSheet(
                    viewModel = viewModel {
                        FeeViewModel(
                            greenWallet = args.greenWallet,
                            accountAssetOrNull = args.accountAsset,
                            isFeeRateOnly = args.isFeeRateOnly,
                            useBreezFees = args.useBreezFees
                        )
                    },
                    onDismissRequest = navController.onDismissRequest()
                )
            }
            appBottomSheet<NavigateDestinations.EnableJadeFeature> {
                val args = it.toRoute<NavigateDestinations.EnableJadeFeature>()
                EnableJadeFeatureBottomSheet(
                    viewModel = viewModel {
                        SimpleGreenViewModel(
                            greenWalletOrNull = args.greenWallet,
                            accountAssetOrNull = args.accountAsset,
                        )
                    },
                    onDismissRequest = navController.onDismissRequest()
                )
            }
            appBottomSheet<NavigateDestinations.DevicePin> {
                PinMatrixBottomSheet(
                    viewModel = viewModel {
                        SimpleGreenViewModel(
                            screenName = "PinMatrix"
                        )
                    },
                    onDismissRequest = navController.onDismissRequest()
                )
            }
            appBottomSheet<NavigateDestinations.DevicePassphrase> {
                PassphraseBottomSheet(
                    viewModel = viewModel {
                        SimpleGreenViewModel(
                            screenName = "PassphraseHW"
                        )
                    },
                    onDismissRequest = navController.onDismissRequest()
                )
            }
            appBottomSheet<NavigateDestinations.BuyQuotes> {
                val args = it.toRoute<NavigateDestinations.BuyQuotes>()
                BuyQuotesBottomSheet(
                    viewModel = viewModel {
                        SimpleGreenViewModel(
                            greenWalletOrNull = args.greenWallet,
                            screenName = "BuyQuotes"
                        )
                    },
                    quotes = args.quotes.quotes ?: emptyList(),
                    selectedServiceProvider = args.selectedServiceProvider,
                    onDismissRequest = navController.onDismissRequest()
                )
            }
            // Dialogs
            appDialog<Dialog> {
                val args = it.toRoute<Dialog>()
                GenericDialog(dialog = args, navController = navController)
            }
            appDialog<NavigateDestinations.UrlWarning>(
                dialogProperties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                )
            ) {
                val args = it.toRoute<NavigateDestinations.UrlWarning>()
                UrlWarningDialog(
                    viewModel = mainViewModel,
                    urls = args.urls,
                    onDismiss = { allow, remember ->
                        mainViewModel.postEvent(
                            MainViewModel.LocalEvents.UrlWarningResponse(
                                allow = allow,
                                remember = remember
                            )
                        )
                        navController.popBackStack(route = args, inclusive = true)
                    }
                )
            }
            appDialog<NavigateDestinations.TorWarning>(
                dialogProperties = DialogProperties()
            ) {
                val args = it.toRoute<NavigateDestinations.TorWarning>()
                TorWarningDialog(viewModel = mainViewModel, onDismiss = {
                    navController.popBackStack(route = args, inclusive = true)
                })
            }
        }
    }
}
