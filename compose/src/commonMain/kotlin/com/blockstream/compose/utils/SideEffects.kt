package com.blockstream.compose.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_are_you_not_receiving_your_2fa
import blockstream_green.common.generated.resources.id_bluetooth
import blockstream_green.common.generated.resources.id_cancel
import blockstream_green.common.generated.resources.id_choose_method_to_authorize_the
import blockstream_green.common.generated.resources.id_contact_support
import blockstream_green.common.generated.resources.id_continue
import blockstream_green.common.generated.resources.id_enable
import blockstream_green.common.generated.resources.id_enable_2fa_call_method
import blockstream_green.common.generated.resources.id_install_version_s
import blockstream_green.common.generated.resources.id_location_services_are_disabled
import blockstream_green.common.generated.resources.id_message_from_recipient_s
import blockstream_green.common.generated.resources.id_new_jade_firmware_available
import blockstream_green.common.generated.resources.id_new_jade_firmware_required
import blockstream_green.common.generated.resources.id_ok
import blockstream_green.common.generated.resources.id_open
import blockstream_green.common.generated.resources.id_outdated_hardware_wallet
import blockstream_green.common.generated.resources.id_payments_will_fail
import blockstream_green.common.generated.resources.id_remove_lightning_shortcut
import blockstream_green.common.generated.resources.id_skip
import blockstream_green.common.generated.resources.id_success
import blockstream_green.common.generated.resources.id_the_new_firmware_requires_you
import blockstream_green.common.generated.resources.id_try_again
import blockstream_green.common.generated.resources.id_try_again_using_another_2fa
import blockstream_green.common.generated.resources.id_warning
import blockstream_green.common.generated.resources.id_you_will_stop_receiving_push
import blockstream_green.common.generated.resources.warning
import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.data.ErrorReport
import com.blockstream.common.data.LogoutReason
import com.blockstream.common.data.TwoFactorMethod
import com.blockstream.common.data.TwoFactorResolverData
import com.blockstream.common.data.TwoFactorSetupAction
import com.blockstream.common.devices.DeviceBrand
import com.blockstream.common.devices.DeviceModel
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.handleException
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.extensions.twoFactorMethodsLocalized
import com.blockstream.common.gdk.data.AssetBalance
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.devices.AbstractDeviceViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.navigation.PopTo
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.StringHolder
import com.blockstream.compose.LocalAppCoroutine
import com.blockstream.compose.LocalBiometricState
import com.blockstream.compose.LocalDialog
import com.blockstream.compose.LocalDrawer
import com.blockstream.compose.LocalRootNavigator
import com.blockstream.compose.LocalSnackbar
import com.blockstream.compose.dialogs.SingleChoiceDialog
import com.blockstream.compose.dialogs.TwoFactorCodeDialog
import com.blockstream.compose.extensions.showErrorSnackbar
import com.blockstream.compose.managers.LocalPlatformManager
import com.blockstream.compose.managers.askForBluetoothPermissions
import com.blockstream.compose.navigation.pushOrReplace
import com.blockstream.compose.navigation.pushUnique
import com.blockstream.compose.screens.HomeScreen
import com.blockstream.compose.screens.about.AboutScreen
import com.blockstream.compose.screens.add.Account2of3Screen
import com.blockstream.compose.screens.add.ChooseAccountTypeScreen
import com.blockstream.compose.screens.add.ReviewAddAccountScreen
import com.blockstream.compose.screens.add.XpubScreen
import com.blockstream.compose.screens.addresses.AddressesScreen
import com.blockstream.compose.screens.archived.ArchivedAccountsScreen
import com.blockstream.compose.screens.devices.DeviceInfoScreen
import com.blockstream.compose.screens.devices.DeviceListScreen
import com.blockstream.compose.screens.devices.DeviceScanScreen
import com.blockstream.compose.screens.devices.ImportPubKeyScreen
import com.blockstream.compose.screens.devices.JadeGenuineCheckScreen
import com.blockstream.compose.screens.exchange.AccountExchangeScreen
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
import com.blockstream.compose.screens.onboarding.phone.AddWalletScreen
import com.blockstream.compose.screens.onboarding.phone.EnterRecoveryPhraseScreen
import com.blockstream.compose.screens.onboarding.phone.PinScreen
import com.blockstream.compose.screens.onboarding.watchonly.WatchOnlyCredentialsScreen
import com.blockstream.compose.screens.onboarding.watchonly.WatchOnlyNetworkScreen
import com.blockstream.compose.screens.onboarding.watchonly.WatchOnlyPolicyScreen
import com.blockstream.compose.screens.overview.AccountOverviewScreen
import com.blockstream.compose.screens.overview.WalletAssetsScreen
import com.blockstream.compose.screens.overview.WalletOverviewScreen
import com.blockstream.compose.screens.promo.PromoScreen
import com.blockstream.compose.screens.receive.ReceiveScreen
import com.blockstream.compose.screens.recovery.RecoveryCheckScreen
import com.blockstream.compose.screens.recovery.RecoveryIntroScreen
import com.blockstream.compose.screens.recovery.RecoveryPhraseScreen
import com.blockstream.compose.screens.recovery.RecoveryWordsScreen
import com.blockstream.compose.screens.send.BumpScreen
import com.blockstream.compose.screens.send.RedepositScreen
import com.blockstream.compose.screens.send.SendConfirmScreen
import com.blockstream.compose.screens.send.SendScreen
import com.blockstream.compose.screens.send.SweepScreen
import com.blockstream.compose.screens.settings.AppSettingsScreen
import com.blockstream.compose.screens.settings.ChangePinScreen
import com.blockstream.compose.screens.settings.TwoFactorAuthenticationScreen
import com.blockstream.compose.screens.settings.TwoFactorSetupScreen
import com.blockstream.compose.screens.settings.WalletSettingsScreen
import com.blockstream.compose.screens.settings.WatchOnlyScreen
import com.blockstream.compose.screens.transaction.TransactionScreen
import com.blockstream.compose.screens.twofactor.ReEnable2FAScreen
import com.blockstream.compose.sheets.AccountRenameBottomSheet
import com.blockstream.compose.sheets.AccountsBottomSheet
import com.blockstream.compose.sheets.AskJadeUnlockBottomSheet
import com.blockstream.compose.sheets.AssetDetailsBottomSheet
import com.blockstream.compose.sheets.AssetsAccountsBottomSheet
import com.blockstream.compose.sheets.AssetsBottomSheet
import com.blockstream.compose.sheets.Bip39PassphraseBottomSheet
import com.blockstream.compose.sheets.Call2ActionBottomSheet
import com.blockstream.compose.sheets.CameraBottomSheet
import com.blockstream.compose.sheets.ChooseAssetAccountBottomSheet
import com.blockstream.compose.sheets.CountriesBottomSheet
import com.blockstream.compose.sheets.DenominationBottomSheet
import com.blockstream.compose.sheets.DeviceInteractionBottomSheet
import com.blockstream.compose.sheets.EnvironmentBottomSheet
import com.blockstream.compose.sheets.FeeRateBottomSheet
import com.blockstream.compose.sheets.JadeFirmwareUpdateBottomSheet
import com.blockstream.compose.sheets.LightningNodeBottomSheet
import com.blockstream.compose.sheets.LocalBottomSheetNavigatorM3
import com.blockstream.compose.sheets.NewJadeConnectedBottomSheet
import com.blockstream.compose.sheets.NfcToastBottomSheet
import com.blockstream.compose.sheets.NoteBottomSheet
import com.blockstream.compose.sheets.PassphraseBottomSheet
import com.blockstream.compose.sheets.PinMatrixBottomSheet
import com.blockstream.compose.sheets.QrBottomSheet
import com.blockstream.compose.sheets.SignMessageBottomSheet
import com.blockstream.compose.sheets.SystemMessageBottomSheet
import com.blockstream.compose.sheets.TransactionDetailsBottomSheet
import com.blockstream.compose.sheets.TwoFactorResetBottomSheet
import com.blockstream.compose.sheets.WalletDeleteBottomSheet
import com.blockstream.compose.sheets.WalletRenameBottomSheet
import com.blockstream.compose.sheets.WatchOnlySettingsCredentialsBottomSheet
import com.blockstream.compose.sideeffects.OpenDialogData
import com.blockstream.compose.sideeffects.openBrowser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@Composable
fun HandleSideEffectDialog(
    viewModel: GreenViewModel,
    onDismiss: CoroutineScope.() -> Unit = {},
    handler: CoroutineScope.(sideEffect: SideEffect) -> Unit = {}
) {
    HandleSideEffect(viewModel, handler = {
        handler.invoke(this, it)

        when (it) {
            is SideEffects.Dismiss -> {
                onDismiss()
            }
        }
    })
}


@Composable
fun HandleSideEffect(
    viewModel: GreenViewModel,
    handler: suspend CoroutineScope.(sideEffect: SideEffect) -> Unit = {}
) {
    val snackbar = LocalSnackbar.current
    val navigator = LocalRootNavigator.current
    val bottomSheetNavigator = LocalBottomSheetNavigatorM3.current
    val dialog = LocalDialog.current
    val appCoroutine = LocalAppCoroutine.current
    val platformManager = LocalPlatformManager.current
    val drawer = LocalDrawer.current
    val scope = rememberCoroutineScope()
    val biometricsState = LocalBiometricState.current

    var twoFactorResolverData by remember { mutableStateOf<TwoFactorResolverData?>(null) }
    twoFactorResolverData?.also { resolverData ->
        resolverData.methods?.also { methods ->
            SingleChoiceDialog(
                title = stringResource(Res.string.id_choose_method_to_authorize_the),
                items = methods.twoFactorMethodsLocalized().map {
                    stringResource(it)
                }
            ) { position ->
                viewModel.postEvent(Events.SelectTwoFactorMethod(method = position?.let { methods.getOrNull(it) }))
                twoFactorResolverData = null
            }
        }

        resolverData.authHandlerStatus?.also { authHandlerStatus ->
            TwoFactorCodeDialog(authHandlerStatus = authHandlerStatus) { code, isHelp ->

                viewModel.postEvent(Events.ResolveTwoFactorCode(code = code))

                if (isHelp == true) {
                    appCoroutine.launch {
                        dialog.openDialog(OpenDialogData(
                            title = StringHolder.create(Res.string.id_are_you_not_receiving_your_2fa),
                            message = StringHolder.create(Res.string.id_try_again_using_another_2fa),
                            primaryText = getString(if (resolverData.enable2faCallMethod) Res.string.id_enable_2fa_call_method else Res.string.id_try_again),
                            secondaryText = getString(Res.string.id_contact_support),
                            onPrimary = {
                                if (resolverData.enable2faCallMethod) {
                                    viewModel.postEvent(
                                        NavigateDestinations.TwoFactorSetup(
                                            method = TwoFactorMethod.PHONE,
                                            action = TwoFactorSetupAction.SETUP,
                                            network = resolverData.network!!,
                                            isSmsBackup = true
                                        )
                                    )
                                }
                            },
                            onSecondary = {
                                appCoroutine.launch {
                                    dialog.openErrorReportDialog(
                                        platformManager = platformManager,
                                        viewModel = viewModel,
                                        errorReport = ErrorReport.createForMultisig("Android: I am not receiving my 2FA code"),
                                        onSubmitErrorReport = { submitErrorReport ->
                                            viewModel.postEvent(submitErrorReport)
                                        }
                                    )
                                }
                            }
                        ))
                    }
                }

                twoFactorResolverData = null
            }
        }
    }

    // Device Passphrase
    PassphraseBottomSheet.getResult {
        viewModel.postEvent(Events.DeviceRequestResponse(it))
    }

    // Device PinMatrix
    PinMatrixBottomSheet.getResult {
        viewModel.postEvent(Events.DeviceRequestResponse(it))
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val state by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()

    // Handle sideEffect only on resumed state
    LaunchedEffect(state) {
        if(state != Lifecycle.State.RESUMED) return@LaunchedEffect

        viewModel.sideEffect.onEach {
            handler.invoke(this, it)

            when (it) {
                is SideEffects.OpenBrowser -> {
                    appCoroutine.launch {
                        openBrowser(
                            platformManager = platformManager,
                            dialogState = dialog,
                            isTor = viewModel.settingsManager.appSettings.tor,
                            url = it.url,
                            openSystemBrowser = it.openSystemBrowser
                        )
                    }
                }

                is SideEffects.RequestCipher -> {
                    biometricsState?.getBiometricsCipher(viewModel)
                }

                is SideEffects.OpenFeeBottomSheet -> {
                    // Pass params to GdkSession
                    viewModel.sessionOrNull?.pendingTransactionParams = it.params

                    bottomSheetNavigator?.show(
                        FeeRateBottomSheet(
                            greenWallet = it.greenWallet,
                            accountAsset = it.accountAsset,
                            useBreezFees = it.useBreezFees
                        )
                    )
                }

                is SideEffects.Snackbar -> {
                    appCoroutine.launch {
                        snackbar.showSnackbar(message = it.text.getString())
                    }
                }

                is SideEffects.OpenDenomination -> {
                    bottomSheetNavigator?.show(
                        DenominationBottomSheet(
                            greenWallet = viewModel.greenWallet,
                            denominatedValue = it.denominatedValue
                        )
                    )
                }

                is SideEffects.ErrorSnackbar -> {
                    appCoroutine.launch {
                        snackbar.showErrorSnackbar(
                            platformManager = platformManager,
                            dialogState = dialog,
                            viewModel = viewModel,
                            error = it.error,
                            errorReport = it.errorReport
                        )
                    }
                }

                is SideEffects.Dialog -> {
                    appCoroutine.launch {
                        dialog.openDialog(
                            OpenDialogData(
                                title = it.title,
                                message = it.message,
                                icon = it.icon
                            )
                        )
                    }
                }

                is SideEffects.AskRemoveLightningShortcut -> {
                    appCoroutine.launch {
                        dialog.openDialog(
                            OpenDialogData(
                                title = StringHolder.create(Res.string.id_payments_will_fail),
                                message = StringHolder.create(Res.string.id_you_will_stop_receiving_push),
                                icon = Res.drawable.warning,
                                primaryText = getString(Res.string.id_remove_lightning_shortcut),
                                secondaryText = getString(Res.string.id_cancel),
                                onPrimary = {
                                    viewModel.postEvent(Events.RemoveLightningShortcut(wallet = it.wallet))
                                },
                                onSecondary = { }
                            )
                        )
                    }
                }

                is SideEffects.TwoFactorResolver -> {
                    twoFactorResolverData = it.data
                }

                is SideEffects.ErrorDialog -> {
                    appCoroutine.launch {
                        dialog.openErrorDialog(
                            throwable = it.error,
                            errorReport = it.errorReport,
                            onErrorReport = { errorReport ->
                                appCoroutine.launch {
                                    dialog.openErrorReportDialog(
                                        platformManager = platformManager,
                                        errorReport = errorReport,
                                        viewModel = viewModel,
                                        onSubmitErrorReport = { submitErrorReport ->
                                            viewModel.postEvent(submitErrorReport)
                                        }
                                    )
                                }
                            })
                    }
                }

                is SideEffects.Share -> {
                    it.text?.also {
                        platformManager.shareText(it)
                    }
                }

                is SideEffects.ShareFile -> {
                    platformManager.shareFile(it.path.toString())
                }

                is SideEffects.NavigateBack -> {
                    // Check if Navigator exists, else is handled by AppFragment for now
                    if (navigator != null) {
                        val error = it.error
                        if (error != null) {
                            appCoroutine.launch {
                                dialog.openErrorDialog(
                                    throwable = error,
                                    errorReport = it.errorReport,
                                    onErrorReport = { errorReport ->
                                        appCoroutine.launch {
                                            dialog.openErrorReportDialog(
                                                platformManager = platformManager,
                                                errorReport = errorReport,
                                                viewModel = viewModel,
                                                onSubmitErrorReport = { submitErrorReport ->
                                                    viewModel.postEvent(submitErrorReport)
                                                }
                                            )
                                        }
                                    }
                                ) {
                                    navigator.pop()
                                }
                            }
                        } else if (it.message != null) {
                            appCoroutine.launch {
                                dialog.openDialog(
                                    OpenDialogData(
                                        title = it.title,
                                        message = it.message,
                                        onPrimary = {
                                            navigator.pop()
                                        }
                                    )
                                )
                            }
                        } else {
                            navigator.pop()
                        }
                    }
                }

                is SideEffects.CopyToClipboard -> {
                    if(!platformManager.copyToClipboard(content = it.value)){
                        it.message?.also {
                            if(!platformManager.openToast(it)) {
                                // In case openToast is not supported
                                appCoroutine.launch {
                                    snackbar.showSnackbar(message = it)
                                }
                            }
                        }
                    }
                }

                is SideEffects.Logout -> {
                    viewModel.greenWalletOrNull?.let { greenWallet ->
                        if (greenWallet.isEphemeral || greenWallet.isHardware || it.reason == LogoutReason.USER_ACTION) {
                            navigator?.replaceAll(HomeScreen)
                        } else {
                            navigator?.replaceAll(
                                LoginScreen(
                                    greenWallet = greenWallet,
                                    isLightningShortcut = greenWallet.isLightning,
                                    autoLoginWallet = !greenWallet.isLightning,
                                )
                            )
                        }
                    } ?: run {
                        navigator?.replaceAll(HomeScreen)
                    }
                }

                is SideEffects.CloseDrawer -> {
                    drawer.close()
                }

                is SideEffects.NavigateToRoot -> {
                    when (it.popTo) {
                        PopTo.Receive -> navigator?.popUntil { it is ReceiveScreen }
                        PopTo.OnOffRamps -> navigator?.popUntil { it is OnOffRampsScreen }
                        PopTo.Root, null -> navigator?.popAll()
                    }
                }

                is SideEffects.TransactionSent -> {
                    // Check if navigator exists or let AppFragment handle it
                    navigator?.also { navigator ->

                        if (it.data.hasMessageOrUrl) {
                            val isUrl = it.data.url.isNotBlank()

                            dialog.openDialog(
                                OpenDialogData(
                                    title = StringHolder.create(Res.string.id_success),
                                    message = StringHolder(string = getString(Res.string.id_message_from_recipient_s,it.data.message ?: "")),
                                    primaryText = getString(if (isUrl) Res.string.id_open else Res.string.id_ok),
                                    secondaryText = if (isUrl) getString(Res.string.id_cancel) else null,
                                    onPrimary = {
                                        if (isUrl) {
                                            appCoroutine.launch {
                                                openBrowser(
                                                    platformManager = platformManager,
                                                    dialogState = dialog,
                                                    isTor = viewModel.settingsManager.appSettings.tor,
                                                    url = it.data.url ?: ""
                                                )
                                            }
                                        }
                                        navigator.popAll()
                                    },
                                    onSecondary = {
                                        navigator.popAll()
                                    },
                                    onDismiss = {
                                        navigator.popAll()
                                    }
                                ))
                        } else {
                            navigator.popAll()
                        }
                    }
                }

                is SideEffects.EnableBluetooth -> {
                    platformManager.enableBluetooth()
                }

                is SideEffects.EnableLocationService -> {
                    dialog.openDialog(
                        OpenDialogData(
                            title = StringHolder(stringResource = Res.string.id_bluetooth),
                            message = StringHolder(stringResource = Res.string.id_location_services_are_disabled),
                            primaryText = getString(Res.string.id_enable),
                            secondaryText = getString(Res.string.id_cancel),
                            onPrimary = {
                                platformManager.enableLocationService()
                            }
                        )
                    )
                }

                is SideEffects.SelectEnvironment -> {
                    bottomSheetNavigator?.show(EnvironmentBottomSheet)
                }

                is SideEffects.DeviceRequestPassphrase -> {
                    bottomSheetNavigator?.show(PassphraseBottomSheet)
                }

                is SideEffects.DeviceRequestPin -> {
                    bottomSheetNavigator?.show(PinMatrixBottomSheet)
                }

                is SideEffects.DeviceRequestNfcToast -> {
                    val screen = bottomSheetNavigator?.show(NfcToastBottomSheet(message = it.message) )

                    scope.launch (context = handleException()) {
                        it.completable?.also { completable ->
                            completable.await()
                        } ?: run { delay(3000L) }

                        bottomSheetNavigator?.hide(screen)
                    }
                }

                is SideEffects.DeviceInteraction -> {
                    val screen = bottomSheetNavigator?.show(
                        DeviceInteractionBottomSheet(
                            deviceId = it.deviceId,
                            isMasterBlindingKeyRequest = it.isMasterBlindingKeyRequest,
                            message = it.message
                        )
                    )

                    scope.launch (context = handleException()) {
                        it.completable?.also { completable ->
                            completable.await()
                        } ?: run { delay(3000L) }

                        bottomSheetNavigator?.hide(screen)
                    }
                }

                is SideEffects.BleRequireRebonding -> {
                    dialog.openDialog(
                        OpenDialogData(
                            title = StringHolder(stringResource = Res.string.id_warning),
                            message = StringHolder(stringResource = Res.string.id_the_new_firmware_requires_you),
                            primaryText = getString(Res.string.id_ok),
                            secondaryText = getString(Res.string.id_cancel),
                            onPrimary = {
                                platformManager.enableLocationService()
                            }
                        )
                    )
                }

                is SideEffects.NavigateTo -> {
                    when (val destination = it.destination) {
                        is NavigateDestinations.About -> {
                            navigator?.pushUnique(
                                AboutScreen
                            )
                        }

                        is NavigateDestinations.SetupNewWallet -> {
                            navigator?.pushUnique(
                                SetupNewWalletScreen
                            )
                        }

                        is NavigateDestinations.AddWallet -> {
                            navigator?.pushUnique(
                                AddWalletScreen
                            )
                        }

                        is NavigateDestinations.EnterRecoveryPhrase -> {
                            navigator?.push(
                                EnterRecoveryPhraseScreen(destination.setupArgs)
                            )
                        }

                        is NavigateDestinations.SetPin -> {
                            navigator?.items?.firstOrNull { screen ->
                                screen is RecoveryIntroScreen
                            }?.also {
                                navigator.popUntil { screen -> screen is RecoveryIntroScreen }
                            }
                            navigator?.push(
                                PinScreen(destination.setupArgs)
                            )
                        }

                        is NavigateDestinations.RecoveryIntro -> {
                            navigator?.push(
                                RecoveryIntroScreen(destination.setupArgs)
                            )
                        }

                        is NavigateDestinations.RecoveryWords -> {
                            navigator?.push(
                                RecoveryWordsScreen(destination.setupArgs)
                            )
                        }

                        is NavigateDestinations.RecoveryCheck -> {
                            navigator?.popUntil { it is RecoveryIntroScreen }
                            navigator?.push(
                                RecoveryCheckScreen(destination.setupArgs)
                            )
                        }

                        is NavigateDestinations.RecoveryPhrase -> {
                            with(destination.setupArgs) {
                                RecoveryPhraseScreen(
                                    isLightning = isLightning,
                                    providedCredentials = credentials,
                                    greenWallet = greenWallet,
                                ).also { screen ->
                                    // WalletSettings > RecoveryIntroScreen
                                    if (navigator?.lastItemOrNull is RecoveryIntroScreen) {
                                        navigator.replace(screen)
                                    } else {
                                        navigator?.push(screen)
                                    }
                                }
                            }
                        }

                        is NavigateDestinations.AppSettings -> {
                            navigator?.pushUnique(
                                AppSettingsScreen
                            )
                        }

                        is NavigateDestinations.WalletSettings -> {
                            navigator?.push(
                                WalletSettingsScreen(
                                    greenWallet = viewModel.greenWallet,
                                    section = destination.section,
                                    network = destination.network
                                )
                            )
                        }

                        is NavigateDestinations.TwoFactorSetup -> {
                            navigator?.push(
                                TwoFactorSetupScreen(
                                    greenWallet = viewModel.greenWallet,
                                    method = destination.method,
                                    action = destination.action,
                                    network = destination.network,
                                    isSmsBackup = destination.isSmsBackup
                                )
                            )
                        }

                        is NavigateDestinations.TwoFactorAuthentication -> {
                            navigator?.push(
                                TwoFactorAuthenticationScreen(
                                    greenWallet = viewModel.greenWallet,
                                    network = destination.network
                                )
                            )
                        }

                        is NavigateDestinations.ChangePin -> {
                            navigator?.push(
                                ChangePinScreen(
                                    greenWallet = viewModel.greenWallet
                                )
                            )
                        }

                        is NavigateDestinations.Camera -> {
                            bottomSheetNavigator?.show(
                                CameraBottomSheet(
                                    isDecodeContinuous = destination.isDecodeContinuous,
                                    parentScreenName = destination.parentScreenName,
                                    setupArgs = destination.setupArgs
                                )
                            )
                        }

                        is NavigateDestinations.Login -> {
                            val loginScreen = LoginScreen(
                                greenWallet = destination.greenWallet,
                                isLightningShortcut = destination.isLightningShortcut,
                                autoLoginWallet = !destination.isLightningShortcut,
                                deviceId = destination.deviceId
                            )

                            navigator?.pushOrReplace(loginScreen)
                        }

                        is NavigateDestinations.Bip39Passphrase -> {
                            bottomSheetNavigator?.show(
                                Bip39PassphraseBottomSheet(
                                    greenWallet = viewModel.greenWallet,
                                    passphrase = destination.passphrase
                                )
                            )
                        }

                        is NavigateDestinations.RenameWallet -> {
                            bottomSheetNavigator?.show(WalletRenameBottomSheet(destination.greenWallet))
                        }

                        is NavigateDestinations.DeleteWallet -> {
                            bottomSheetNavigator?.show(WalletDeleteBottomSheet(destination.greenWallet))
                        }

                        is NavigateDestinations.WalletOverview -> {
                            navigator?.replaceAll(
                                WalletOverviewScreen(
                                    greenWallet = destination.greenWallet,
                                )
                            )
                        }

                        is NavigateDestinations.AccountOverview -> {
                            navigator?.pushUnique(
                                AccountOverviewScreen(
                                    greenWallet = viewModel.greenWallet,
                                    accountAsset = destination.accountAsset
                                )
                            )
                        }

                        is NavigateDestinations.AssetDetails -> {
                            bottomSheetNavigator?.show(
                                AssetDetailsBottomSheet(
                                    greenWallet = viewModel.greenWallet,
                                    assetId = destination.assetId,
                                    accountAsset = destination.accountAsset
                                )
                            )
                        }

                        is NavigateDestinations.Transaction -> {
                            navigator?.push(
                                TransactionScreen(
                                    greenWallet = viewModel.greenWallet,
                                    transaction = destination.transaction,
                                )
                            )
                        }

                        is NavigateDestinations.TransactionDetails -> {
                            bottomSheetNavigator?.show(
                                TransactionDetailsBottomSheet(
                                    greenWallet = viewModel.greenWallet,
                                    transaction = destination.transaction
                                )
                            )
                        }

                        is NavigateDestinations.Addresses -> {
                            navigator?.push(
                                AddressesScreen(
                                    greenWallet = viewModel.greenWallet,
                                    accountAsset = destination.accountAsset
                                )
                            )
                        }

                        is NavigateDestinations.Send -> {
                            navigator?.push(
                                SendScreen(
                                    greenWallet = viewModel.greenWallet,
                                    address = destination.address,
                                    addressInputType = destination.addressType
                                )
                            )
                        }

                        is NavigateDestinations.ArchivedAccounts -> {
                            navigator?.pushUnique(
                                ArchivedAccountsScreen(
                                    greenWallet = viewModel.greenWallet,
                                    navigateToRoot = destination.navigateToRoot
                                )
                            )
                        }

                        is NavigateDestinations.AssetsAccounts -> {
                            bottomSheetNavigator?.show(
                                AssetsAccountsBottomSheet(
                                    greenWallet = viewModel.greenWallet,
                                    assetsAccounts = destination.assetsAccounts
                                )
                            )
                        }

                        is NavigateDestinations.RenameAccount -> {
                            bottomSheetNavigator?.show(
                                AccountRenameBottomSheet(
                                    greenWallet = viewModel.greenWallet,
                                    account = destination.account
                                )
                            )
                        }

                        is NavigateDestinations.Accounts -> {
                            bottomSheetNavigator?.show(
                                AccountsBottomSheet(
                                    greenWallet = viewModel.greenWallet,
                                    accountsBalance = destination.accounts,
                                    withAsset = destination.withAsset
                                )
                            )
                        }

                        is NavigateDestinations.Assets -> {
                            val assets = destination.assets ?: withContext(context = Dispatchers.IO) {
                                val session = viewModel.session
                                (listOfNotNull(
                                    EnrichedAsset.createOrNull(session = session, session.bitcoin?.policyAsset),
                                    EnrichedAsset.createOrNull(session = session, session.liquid?.policyAsset),
                                ) + (session.enrichedAssets.value.takeIf { session.liquid != null }?.map {
                                    EnrichedAsset.create(session = session, assetId = it.assetId)
                                } ?: listOf()) + listOfNotNull(
                                    EnrichedAsset.createAnyAsset(session = session, isAmp = false),
                                    EnrichedAsset.createAnyAsset(session = session, isAmp = true)
                                ).sortedWith(session::sortEnrichedAssets)).let { list ->
                                    list.map {
                                        AssetBalance.create(it)
                                    }
                                }
                            }


                            bottomSheetNavigator?.show(
                                AssetsBottomSheet(
                                    greenWallet = viewModel.greenWallet,
                                    assetBalance = assets
                                )
                            )
                        }

                        is NavigateDestinations.Qr -> {
                            bottomSheetNavigator?.show(
                                QrBottomSheet(
                                    greenWallet = viewModel.greenWallet,
                                    title = destination.title,
                                    subtitle = destination.subtitle,
                                    data = destination.data
                                )
                            )
                        }
                        is NavigateDestinations.WatchOnlyCredentialsSettings -> {
                            bottomSheetNavigator?.show(
                                WatchOnlySettingsCredentialsBottomSheet(
                                    greenWallet = viewModel.greenWallet,
                                    network = destination.network
                                )
                            )
                        }




                        is NavigateDestinations.EnableTwoFactor -> {
                            bottomSheetNavigator?.show(
                                Call2ActionBottomSheet(
                                    greenWallet = viewModel.greenWallet,
                                    network = destination.network
                                )
                            )
                        }

                        is NavigateDestinations.LightningNode -> {
                            bottomSheetNavigator?.show(
                                LightningNodeBottomSheet(
                                    viewModel.greenWallet,
                                )
                            )
                        }

                        is NavigateDestinations.SignMessage -> {
                            bottomSheetNavigator?.show(
                                SignMessageBottomSheet(
                                    greenWallet = viewModel.greenWallet,
                                    accountAsset = destination.accountAsset,
                                    address = destination.address
                                )
                            )
                        }

                        is NavigateDestinations.SystemMessage -> {
                            bottomSheetNavigator?.show(
                                SystemMessageBottomSheet(
                                    greenWallet = viewModel.greenWallet,
                                    network = destination.network,
                                    message = destination.message
                                )
                            )
                        }

                        is NavigateDestinations.TwoFactorReset -> {
                            bottomSheetNavigator?.show(
                                TwoFactorResetBottomSheet(
                                    greenWallet = viewModel.greenWallet,
                                    network = destination.network
                                )
                            )
                        }

                        is NavigateDestinations.DeviceInteraction -> {
                            bottomSheetNavigator?.show(
                                DeviceInteractionBottomSheet(
                                    greenWalletOrNull = viewModel.greenWalletOrNull,
                                    deviceId = destination.deviceId,
                                    transactionConfirmLook = destination.transactionConfirmLook,
                                    verifyAddress = destination.verifyAddress,
                                    isMasterBlindingKeyRequest = destination.isMasterBlindingKeyRequest,
                                    message = destination.message
                                )
                            )
                        }

                        is NavigateDestinations.Note -> {
                            bottomSheetNavigator?.show(
                                NoteBottomSheet(
                                    greenWallet = viewModel.greenWallet,
                                    note = destination.note,
                                    noteType = destination.noteType
                                )
                            )
                        }

                        is NavigateDestinations.LnUrlAuth -> {
                            navigator?.push(
                                LnUrlAuthScreen(
                                    greenWallet = viewModel.greenWallet,
                                    requestData = destination.lnUrlAuthRequest,
                                )
                            )
                        }

                        is NavigateDestinations.LnUrlWithdraw -> {
                            navigator?.push(
                                LnUrlWithdrawScreen(
                                    greenWallet = viewModel.greenWallet,
                                    requestData = destination.lnUrlWithdrawRequest,
                                )
                            )
                        }

                        is NavigateDestinations.RecoverFunds -> {
                            navigator?.push(
                                RecoverFundsScreen(
                                    greenWallet = viewModel.greenWallet,
                                    isSendAll = destination.isSendAll,
                                    amount = destination.amount,
                                    address = destination.address
                                )
                            )
                        }

                        is NavigateDestinations.JadeQR -> {
                            navigator?.push(
                                JadeQRScreen(
                                    greenWallet = viewModel.greenWalletOrNull,
                                    operation = destination.operation,
                                    deviceModel = destination.deviceModel ?: viewModel.sessionOrNull?.deviceModel ?: DeviceModel.Generic
                                )
                            )
                        }

                        is NavigateDestinations.AskJadeUnlock -> {
                            bottomSheetNavigator?.push(AskJadeUnlockBottomSheet(isOnboarding = destination.isOnboarding))
                        }

                        is NavigateDestinations.NewJadeConnected -> {
                            bottomSheetNavigator?.push(NewJadeConnectedBottomSheet)
                        }

                        is NavigateDestinations.JadePinUnlock -> {
                            navigator?.push(JadePinUnlockScreen)
                        }

                        is NavigateDestinations.ImportPubKey -> {
                            ImportPubKeyScreen(deviceModel = destination.deviceModel).also { screen ->
                                if (navigator?.lastItemOrNull is JadePinUnlockScreen) {
                                    navigator.replace(screen)
                                } else {
                                    navigator?.push(screen)
                                }
                            }
                        }

                        is NavigateDestinations.WatchOnlyPolicy -> {
                            navigator?.pushUnique(
                                WatchOnlyPolicyScreen
                            )
                        }

                        is NavigateDestinations.WatchOnlyNetwork -> {
                            navigator?.pushUnique(
                                WatchOnlyNetworkScreen(destination.setupArgs)
                            )
                        }

                        is NavigateDestinations.WatchOnlyCredentials -> {
                            navigator?.pushUnique(
                                WatchOnlyCredentialsScreen(destination.setupArgs)
                            )
                        }

                        is NavigateDestinations.UseHardwareDevice -> {
                            navigator?.pushUnique(
                                UseHardwareDeviceScreen
                            )
                        }

                        is NavigateDestinations.Sweep -> {
                            navigator?.push(
                                SweepScreen(
                                    greenWallet = viewModel.greenWallet,
                                    privateKey = destination.privateKey,
                                    accountAsset = destination.accountAsset
                                )
                            )
                        }

                        is NavigateDestinations.Bump -> {
                            navigator?.push(
                                BumpScreen(
                                    greenWallet = viewModel.greenWallet,
                                    accountAsset = destination.accountAsset,
                                    transaction = destination.transaction
                                )
                            )
                        }

                        is NavigateDestinations.SendConfirm -> {
                            navigator?.push(
                                SendConfirmScreen(
                                    greenWallet = viewModel.greenWallet,
                                    accountAsset = destination.accountAsset,
                                    denomination = destination.denomination
                                )
                            )
                        }

                        is NavigateDestinations.AccountExchange -> {
                            navigator?.push(
                                AccountExchangeScreen(
                                    greenWallet = viewModel.greenWallet,
                                )
                            )
                        }

                        is NavigateDestinations.OnOffRamps -> {
                            navigator?.push(
                                OnOffRampsScreen(
                                    greenWallet = viewModel.greenWallet,
                                )
                            )
                        }

                        is NavigateDestinations.Redeposit -> {
                            navigator?.push(
                                RedepositScreen(
                                    greenWallet = viewModel.greenWallet,
                                    accountAsset = destination.accountAsset,
                                    isRedeposit2FA = destination.isRedeposit2FA
                                )
                            )
                        }

                        is NavigateDestinations.ReEnable2FA -> {
                            navigator?.push(
                                ReEnable2FAScreen(
                                    greenWallet = viewModel.greenWallet
                                )
                            )
                        }

                        is NavigateDestinations.Countries -> {
                            bottomSheetNavigator?.push(
                                CountriesBottomSheet(
                                    greenWallet = viewModel.greenWallet
                                )
                            )
                        }

                        is NavigateDestinations.WalletAssets -> {
                            navigator?.pushUnique(
                                WalletAssetsScreen(
                                    greenWallet = viewModel.greenWallet
                                )
                            )
                        }

                        is NavigateDestinations.ChooseAccountType -> {
                            navigator?.pushUnique(
                                ChooseAccountTypeScreen(
                                    greenWallet = viewModel.greenWallet,
                                    assetBalance = destination.assetBalance,
                                    allowAssetSelection = destination.allowAssetSelection,
                                    popTo = destination.popTo
                                )
                            )
                        }

                        is NavigateDestinations.Xpub -> {
                            navigator?.pushUnique(
                                XpubScreen(
                                    greenWallet = viewModel.greenWallet,
                                    setupArgs = destination.setupArgs,
                                )
                            )
                        }

                        is NavigateDestinations.AddAccount2of3 -> {
                            navigator?.pushUnique(
                                Account2of3Screen(
                                    greenWallet = viewModel.greenWallet,
                                    setupArgs = destination.setupArgs,
                                )
                            )
                        }

                        is NavigateDestinations.ReviewAddAccount -> {
                            // Clear all previous Recovery screens if needed
                            navigator?.popUntil { it is RecoveryIntroScreen }
                            navigator?.pushUnique(
                                ReviewAddAccountScreen(
                                    greenWallet = viewModel.greenWallet,
                                    setupArgs = destination.setupArgs,
                                )
                            )
                        }

                        is NavigateDestinations.WatchOnly -> {
                            navigator?.push(
                                WatchOnlyScreen(
                                    greenWallet = viewModel.greenWallet
                                )
                            )
                        }

                        is NavigateDestinations.JadeGuide -> {
                            navigator?.push(
                                JadeGuideScreen
                            )
                        }

                        is NavigateDestinations.ChooseAssetAccounts -> {
                            bottomSheetNavigator?.push(
                                ChooseAssetAccountBottomSheet(greenWallet = viewModel.greenWallet)
                            )
                        }

                        is NavigateDestinations.Receive -> {
                            navigator?.push(
                                ReceiveScreen(
                                    greenWallet = viewModel.greenWallet,
                                    accountAsset = destination.accountAsset
                                )
                            )
                        }

                        is NavigateDestinations.Promo -> {
                            navigator?.push(
                                PromoScreen(
                                    promo = destination.promo,
                                    greenWallet = viewModel.greenWalletOrNull
                                )
                            )
                        }

                        is NavigateDestinations.DeviceList -> {
                            navigator?.push(
                                DeviceListScreen(
                                    isJade = destination.isJade
                                )
                            )
                        }

                        is NavigateDestinations.DeviceInfo -> {
                            navigator?.push(
                                DeviceInfoScreen(
                                    deviceId = destination.deviceId
                                )
                            )
                        }

                        is NavigateDestinations.JadeGenuineCheck -> {
                            navigator?.push(
                                JadeGenuineCheckScreen(
                                    greenWallet = viewModel.greenWalletOrNull,
                                    deviceId = destination.deviceId
                                )
                            )
                        }

                        is NavigateDestinations.DeviceScan -> {
                            navigator?.push(
                                DeviceScanScreen(
                                    greenWallet = destination.greenWallet
                                )
                            )
                        }

                        is NavigateDestinations.JadeFirmwareUpdate -> {
                            bottomSheetNavigator?.push(
                                JadeFirmwareUpdateBottomSheet(
                                    deviceId = destination.deviceId
                                )
                            )
                        }

                    }
                }
            }
        }.collect()
    }
}

@Composable
fun DeviceHandleSideEffect(
    viewModel: AbstractDeviceViewModel,
    handler: suspend CoroutineScope.(sideEffect: SideEffect) -> Unit = {}
) {
    val dialog = LocalDialog.current
    val appCoroutine = LocalAppCoroutine.current
    var askForBluetoothPermissions by remember { mutableStateOf(false) }

    if (askForBluetoothPermissions) {
        askForBluetoothPermissions(viewModel) {
            askForBluetoothPermissions = false
        }
    }

    HandleSideEffect(viewModel = viewModel, handler = {
        if (it is SideEffects.AskForBluetoothPermissions) {
            askForBluetoothPermissions = true
        } else if(it is AbstractDeviceViewModel.LocalSideEffects.AskForFirmwareUpgrade) {

            if(viewModel.deviceOrNull?.deviceBrand == DeviceBrand.Blockstream) {

                val title = when {
                    it.request.firmwareList != null -> "Select firmware"
                    it.request.isUpgradeRequired -> getString(Res.string.id_new_jade_firmware_required)
                    else -> getString(Res.string.id_new_jade_firmware_available)
                }

                val message = if (it.request.firmwareList == null) getString(
                    Res.string.id_install_version_s,
                    it.request.upgradeVersion ?: ""
                ) else null

                appCoroutine.launch {
                    dialog.openDialog(OpenDialogData(
                        title = StringHolder.create(title),
                        message = message?.let { StringHolder.create(it) },
                        items = it.request.firmwareList,
                        primaryText = getString(Res.string.id_continue) ,
                        secondaryText = if (it.request.firmwareList == null) getString(if (it.request.isUpgradeRequired) Res.string.id_cancel else Res.string.id_skip) else null,
                        onPrimary = {
                            viewModel.postEvent(AbstractDeviceViewModel.LocalEvents.RespondToFirmwareUpgrade(index = 0))
                        },
                        onSecondary = {
                            viewModel.postEvent(AbstractDeviceViewModel.LocalEvents.RespondToFirmwareUpgrade(index = null))
                        }, onItem = {
                            if(it != null) {
                                viewModel.postEvent(
                                    AbstractDeviceViewModel.LocalEvents.RespondToFirmwareUpgrade(
                                        index = it
                                    )
                                )
                            }
                        }
                    ))
                }

            } else if(viewModel.deviceOrNull != null && it.request.isUpgradeRequired){

                appCoroutine.launch {
                    dialog.openDialog(OpenDialogData(
                        title = StringHolder.create(Res.string.id_warning),
                        message = StringHolder.create(Res.string.id_outdated_hardware_wallet),
                        primaryText = getString(Res.string.id_continue) ,
                        secondaryText = getString(Res.string.id_cancel),
                        onPrimary = {
                            viewModel.postEvent(AbstractDeviceViewModel.LocalEvents.RespondToFirmwareUpgrade(index = 0))
                        },
                        onSecondary = {
                            viewModel.postEvent(AbstractDeviceViewModel.LocalEvents.RespondToFirmwareUpgrade(index = 0))
                        }
                    ))
                }
            }

        } else {
            handler.invoke(this, it)
        }
    })
}