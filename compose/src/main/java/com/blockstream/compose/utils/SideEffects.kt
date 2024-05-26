package com.blockstream.compose.utils

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import com.blockstream.common.data.ErrorReport
import com.blockstream.common.data.LogoutReason
import com.blockstream.common.data.TwoFactorMethod
import com.blockstream.common.data.TwoFactorResolverData
import com.blockstream.common.data.TwoFactorSetupAction
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.extensions.twoFactorMethodsLocalized
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.LocalAppCoroutine
import com.blockstream.compose.LocalDialog
import com.blockstream.compose.LocalRootNavigator
import com.blockstream.compose.LocalSnackbar
import com.blockstream.compose.R
import com.blockstream.compose.dialogs.SingleChoiceDialog
import com.blockstream.compose.dialogs.TwoFactorCodeDialog
import com.blockstream.compose.extensions.showErrorSnackbar
import com.blockstream.compose.navigation.pushOrReplace
import com.blockstream.compose.navigation.pushUnique
import com.blockstream.compose.screens.HomeScreen
import com.blockstream.compose.screens.about.AboutScreen
import com.blockstream.compose.screens.addresses.AddressesScreen
import com.blockstream.compose.screens.archived.ArchivedAccountsScreen
import com.blockstream.compose.screens.lightning.LnUrlAuthScreen
import com.blockstream.compose.screens.lightning.LnUrlWithdrawScreen
import com.blockstream.compose.screens.lightning.RecoverFundsScreen
import com.blockstream.compose.screens.login.LoginScreen
import com.blockstream.compose.screens.onboarding.SetupNewWalletScreen
import com.blockstream.compose.screens.onboarding.hardware.UseHardwareDeviceScreen
import com.blockstream.compose.screens.onboarding.phone.AddWalletScreen
import com.blockstream.compose.screens.onboarding.phone.EnterRecoveryPhraseScreen
import com.blockstream.compose.screens.onboarding.phone.PinScreen
import com.blockstream.compose.screens.onboarding.watchonly.WatchOnlyCredentialsScreen
import com.blockstream.compose.screens.onboarding.watchonly.WatchOnlyNetworkScreen
import com.blockstream.compose.screens.onboarding.watchonly.WatchOnlyPolicyScreen
import com.blockstream.compose.screens.overview.AccountOverviewScreen
import com.blockstream.compose.screens.overview.WalletOverviewScreen
import com.blockstream.compose.screens.receive.ReceiveScreen
import com.blockstream.compose.screens.recovery.RecoveryCheckScreen
import com.blockstream.compose.screens.recovery.RecoveryIntroScreen
import com.blockstream.compose.screens.recovery.RecoveryPhraseScreen
import com.blockstream.compose.screens.recovery.RecoveryWordsScreen
import com.blockstream.compose.screens.send.AccountExchangeScreen
import com.blockstream.compose.screens.send.BumpScreen
import com.blockstream.compose.screens.send.RedepositScreen
import com.blockstream.compose.screens.send.SendConfirmScreen
import com.blockstream.compose.screens.send.SendScreen
import com.blockstream.compose.screens.send.SweepScreen
import com.blockstream.compose.screens.settings.AppSettingsScreen
import com.blockstream.compose.screens.settings.WalletSettingsScreen
import com.blockstream.compose.screens.transaction.TransactionScreen
import com.blockstream.compose.screens.twofactor.ReEnable2FAScreen
import com.blockstream.compose.sheets.AccountRenameBottomSheet
import com.blockstream.compose.sheets.AccountsBottomSheet
import com.blockstream.compose.sheets.AssetDetailsBottomSheet
import com.blockstream.compose.sheets.AssetsAccountsBottomSheet
import com.blockstream.compose.sheets.Bip39PassphraseBottomSheet
import com.blockstream.compose.sheets.Call2ActionBottomSheet
import com.blockstream.compose.sheets.CameraBottomSheet
import com.blockstream.compose.sheets.DenominationBottomSheet
import com.blockstream.compose.sheets.FeeRateBottomSheet
import com.blockstream.compose.sheets.LightningNodeBottomSheet
import com.blockstream.compose.sheets.LocalBottomSheetNavigatorM3
import com.blockstream.compose.sheets.SignMessageBottomSheet
import com.blockstream.compose.sheets.SystemMessageBottomSheet
import com.blockstream.compose.sheets.TransactionDetailsBottomSheet
import com.blockstream.compose.sheets.TwoFactorResetBottomSheet
import com.blockstream.compose.sheets.VerifyTransactionBottomSheet
import com.blockstream.compose.sheets.WalletDeleteBottomSheet
import com.blockstream.compose.sheets.WalletRenameBottomSheet
import com.blockstream.compose.sideeffects.OpenDialogData
import com.blockstream.compose.sideeffects.openBrowser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun HandleSideEffectDialog(viewModel: GreenViewModel, onDismiss: CoroutineScope.() -> Unit = {}, handler: CoroutineScope.(sideEffect: SideEffect) -> Unit = {}) {
    LaunchedEffect(Unit) {
        viewModel.sideEffect.onEach {
            handler.invoke(this, it)

            when (it) {
                is SideEffects.Dismiss -> {
                    onDismiss()
                }
            }
        }.collect()
    }
}

@Composable
fun HandleSideEffect(viewModel: GreenViewModel, handler: CoroutineScope.(sideEffect: SideEffect) -> Unit = {}) {
    val snackbar = LocalSnackbar.current
    val navigator = LocalRootNavigator.current
    val bottomSheetNavigator = LocalBottomSheetNavigatorM3.current
    val context = LocalContext.current
    val dialog = LocalDialog.current
    val appCoroutine = LocalAppCoroutine.current

    var twoFactorResolverData by remember { mutableStateOf<TwoFactorResolverData?>(null) }
    twoFactorResolverData?.also {
        it.methods?.also { methods ->
            SingleChoiceDialog(
                title = stringResource(id = R.string.id_choose_method_to_authorize_the),
                items = methods.twoFactorMethodsLocalized()
            ) {
                if(it == null){
                    viewModel._twoFactorDeferred?.completeExceptionally(Exception("id_action_canceled"))
                }else {
                    methods.getOrNull(it)?.also {
                        viewModel._twoFactorDeferred?.complete(it)
                    }
                }
                twoFactorResolverData = null
            }
        }

        it.authHandlerStatus?.also { authHandlerStatus ->
            TwoFactorCodeDialog(authHandlerStatus = authHandlerStatus){ code, isHelp ->
                if(code != null){
                    viewModel._twoFactorDeferred?.complete(code)
                }else{
                    viewModel._twoFactorDeferred?.completeExceptionally(Exception("id_action_canceled"))

                    if(isHelp == true){
                        appCoroutine.launch {
                            dialog.openDialog(OpenDialogData(
                                title = "id_are_you_not_receiving_your_2fa_code",
                                message = "id_try_again_using_another_2fa_method",
                                primaryText = if (it.enable2faCallMethod) "id_enable_2fa_call_method" else "id_try_again",
                                secondaryText = "id_contact_support",
                                onPrimary = {
                                    if (it.enable2faCallMethod){
                                        viewModel.postEvent(NavigateDestinations.TwoFactorSetup(
                                            method = TwoFactorMethod.PHONE,
                                            action = TwoFactorSetupAction.SETUP,
                                            network = it.network!!,
                                            isSmsBackup = true
                                        ))
                                    }
                                },
                                onSecondary = {
                                    appCoroutine.launch {
                                        dialog.openErrorReportDialog(
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
                }

                twoFactorResolverData = null
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.sideEffect.onEach {
            handler.invoke(this, it)

            when (it) {
                is SideEffects.OpenBrowser -> {
                    appCoroutine.launch {
                        openBrowser(
                            context = context,
                            dialogState = dialog,
                            isTor = viewModel.settingsManager.appSettings.tor,
                            url = it.url
                        )
                    }
                }

                is SideEffects.OpenFeeBottomSheet -> {
                    bottomSheetNavigator.show(
                        FeeRateBottomSheet(
                            greenWallet = it.greenWallet,
                            accountAsset = it.accountAsset,
                            params = it.params,
                            useBreezFees = it.useBreezFees
                        )
                    )
                }

                is SideEffects.Snackbar -> {
                    appCoroutine.launch {
                        snackbar.showSnackbar(message = stringResourceId(context, it.text))
                    }
                }

                is SideEffects.OpenDenomination -> {
                    bottomSheetNavigator.show(
                        DenominationBottomSheet(
                            greenWallet = viewModel.greenWallet,
                            denominatedValue = it.denominatedValue
                        )
                    )
                }

                is SideEffects.ErrorSnackbar -> {
                    appCoroutine.launch {
                        snackbar.showErrorSnackbar(
                            context = context,
                            dialogState = dialog,
                            viewModel = viewModel,
                            error = it.error,
                            errorReport = it.errorReport
                        )
                    }
                }

                is SideEffects.Dialog -> {
                    appCoroutine.launch {
                        dialog.openDialog(OpenDialogData(stringResourceIdOrNull(context, it.title), stringResourceId(context, it.message)))
                    }
                }

                is SideEffects.TwoFactorResolver -> {
                    twoFactorResolverData = it.data
                }

                is SideEffects.ErrorDialog -> {
                    appCoroutine.launch {
                        dialog.openErrorDialog(it.error, it.errorReport, onErrorReport = { errorReport ->
                            appCoroutine.launch {
                                dialog.openErrorReportDialog(
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
                    it.text?.also { text ->
                        val builder = ShareCompat.IntentBuilder(context)
                            .setType("text/plain")
                            .setText(text)

                        context.startActivity(
                            Intent.createChooser(
                                builder.intent,
                                context.getString(R.string.id_share)
                            )
                        )
                    }
                }

                is SideEffects.ShareFile -> {
                    val fileUri = FileProvider.getUriForFile(
                        context,
                        context.packageName.toString() + ".provider",
                        File(it.path.toString())
                    )

                    val builder = ShareCompat.IntentBuilder(context)
                        .setType("text/plain")
                        .setStream(fileUri)

                    context.startActivity(
                        Intent.createChooser(
                            builder.intent,
                            context.getString(R.string.id_share)
                        )
                    )
                }

                is SideEffects.NavigateBack -> {
                    // Check if Navigator exists, else is handled by AppFragment for now
                    if(navigator != null) {
                        val error = it.error
                        if (error != null) {
                            appCoroutine.launch {
                                dialog.openErrorDialog(
                                    error,
                                    it.errorReport,
                                    onErrorReport = { errorReport ->
                                        appCoroutine.launch {
                                            dialog.openErrorReportDialog(
                                                errorReport = errorReport,
                                                viewModel = viewModel,
                                                onSubmitErrorReport = { submitErrorReport ->
                                                    viewModel.postEvent(submitErrorReport)
                                                }
                                            )
                                        }
                                    }) {
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
                    copyToClipboard(context = context, "Green", it.value)
                    it.message?.also {
                        appCoroutine.launch {
                            snackbar.showSnackbar(message = stringResourceId(context = context, id = it))
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
                                    autoLoginWallet = !greenWallet.isLightning
                                )
                            )
                        }
                    } ?: run {
                        navigator?.replaceAll(HomeScreen)
                    }
                }

                is SideEffects.NavigateToRoot -> {
                    navigator?.popAll()
                }

                is SideEffects.TransactionSent -> {
                    // Check if navigator exists or let AppFragment handle it
                    navigator?.also { navigator ->

                        if (it.data.hasMessageOrUrl) {
                            val message = "id_message_from_recipient_s|${it.data.message ?: ""}"
                            val isUrl = it.data.url.isNotBlank()

                            dialog.openDialog(
                                OpenDialogData(
                                    title = "id_success",
                                    message = message,
                                    primaryText = if(isUrl) "id_open" else "id_ok",
                                    secondaryText = if(isUrl) "id_cancel" else null,
                                    onPrimary = {
                                        if(isUrl){
                                            appCoroutine.launch {
                                                openBrowser(
                                                    context = context,
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

                is SideEffects.NavigateTo -> {
                    when(val destination = it.destination) {
                        is NavigateDestinations.LightningNode -> {
                            bottomSheetNavigator.show(
                                LightningNodeBottomSheet(
                                    viewModel.greenWallet,
                                ).also {
                                    it.parentViewModel = viewModel
                                }
                            )
                        }

                        is NavigateDestinations.TransactionDetails -> {
                            bottomSheetNavigator.show(
                                TransactionDetailsBottomSheet(
                                    greenWallet = viewModel.greenWallet,
                                    transaction = destination.transaction
                                )
                            )
                        }

                        is NavigateDestinations.AssetDetails -> {
                            bottomSheetNavigator.show(
                                AssetDetailsBottomSheet(
                                    greenWallet = viewModel.greenWallet,
                                    assetId = destination.assetId,
                                    accountAsset = destination.accountAsset
                                )
                            )
                        }

                        is NavigateDestinations.RenameAccount -> {
                            bottomSheetNavigator.show(
                                AccountRenameBottomSheet(
                                    viewModel.greenWallet,
                                    destination.account
                                )
                            )
                        }

                        is NavigateDestinations.SystemMessage -> {
                            bottomSheetNavigator.show(
                                SystemMessageBottomSheet(
                                    greenWallet = viewModel.greenWallet,
                                    network = destination.network,
                                    message = destination.message
                                )
                            )
                        }

                        is NavigateDestinations.TwoFactorReset -> {
                            bottomSheetNavigator.show(
                                TwoFactorResetBottomSheet(
                                    greenWallet = viewModel.greenWallet,
                                    network = destination.network,
                                    twoFactorReset = destination.twoFactorReset,
                                ).also {
                                    it.parentViewModel = viewModel
                                }
                            )
                        }
                        
                        is NavigateDestinations.RenameWallet -> {
                            bottomSheetNavigator.show(WalletRenameBottomSheet(destination.greenWallet))
                        }

                        is NavigateDestinations.AssetsAccounts -> {
                            bottomSheetNavigator.show(AssetsAccountsBottomSheet(destination.greenWallet, destination.assetsAccounts))
                        }

                        is NavigateDestinations.Accounts -> {
                            bottomSheetNavigator.show(
                                AccountsBottomSheet(
                                    greenWallet = destination.greenWallet,
                                    accountsBalance = destination.accounts,
                                    withAsset = destination.withAsset
                                )
                            )
                        }

                        is NavigateDestinations.DeleteWallet -> {
                            bottomSheetNavigator.show(WalletDeleteBottomSheet(destination.greenWallet))
                        }

                        is NavigateDestinations.Bip39Passphrase -> {
                            bottomSheetNavigator.show(
                                Bip39PassphraseBottomSheet(
                                    greenWallet = viewModel.greenWallet,
                                    passphrase = destination.passphrase
                                )
                            )
                        }

                        is NavigateDestinations.EnableTwoFactor -> {
                            bottomSheetNavigator.show(
                                Call2ActionBottomSheet(greenWallet = viewModel.greenWallet, network = destination.network).also {
                                    it.parentViewModel = viewModel
                                }
                            )
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

                        is NavigateDestinations.ArchivedAccounts -> {
                            navigator?.pushUnique(
                                ArchivedAccountsScreen(
                                    greenWallet = viewModel.greenWallet,
                                    navigateToRoot = destination.navigateToRoot
                                )
                            )
                        }

                        is NavigateDestinations.WalletSettings -> {
                            navigator?.push(
                                WalletSettingsScreen(
                                    greenWallet = viewModel.greenWallet
                                )
                            )
                        }

                        is NavigateDestinations.WalletLogin -> {
                            val loginScreen = LoginScreen(
                                greenWallet = destination.greenWallet,
                                isLightningShortcut = destination.isLightningShortcut,
                                autoLoginWallet = !destination.isLightningShortcut,
                                // device = null
                            )

                            navigator?.pushOrReplace(loginScreen)
                        }

                        is NavigateDestinations.DeviceScan -> {

                        }

                        is NavigateDestinations.AppSettings -> {
                            navigator?.pushUnique(
                                AppSettingsScreen
                            )
                        }

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

                        is NavigateDestinations.UseHardwareDevice -> {
                            navigator?.pushUnique(
                                UseHardwareDeviceScreen
                            )
                        }

                        is NavigateDestinations.WatchOnlyPolicy -> {
                            navigator?.pushUnique(
                                WatchOnlyPolicyScreen
                            )
                        }

                        is NavigateDestinations.WatchOnlyNetwork -> {
                            navigator?.pushUnique(
                                WatchOnlyNetworkScreen(destination.args)
                            )
                        }

                        is NavigateDestinations.WatchOnlyCredentials -> {
                            navigator?.pushUnique(
                                WatchOnlyCredentialsScreen(destination.args)
                            )
                        }

                        is NavigateDestinations.RecoveryIntro -> {
                            navigator?.push(
                                RecoveryIntroScreen(destination.args)
                            )
                        }

                        is NavigateDestinations.RecoveryWords -> {
                            navigator?.push(
                                RecoveryWordsScreen(destination.args)
                            )
                        }

                        is NavigateDestinations.RecoveryCheck -> {
                            navigator?.popUntil { it is RecoveryIntroScreen}
                            navigator?.push(
                                RecoveryCheckScreen(destination.args)
                            )
                        }

                        is NavigateDestinations.SetPin -> {
                            navigator?.items?.firstOrNull { screen ->
                                screen is RecoveryIntroScreen
                            }?.also {
                                navigator.popUntil { screen -> screen is RecoveryIntroScreen}
                            }
                            navigator?.push(
                                PinScreen(destination.args)
                            )
                        }

                        is NavigateDestinations.EnterRecoveryPhrase -> {
                            navigator?.push(
                                EnterRecoveryPhraseScreen(destination.args)
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

                        is NavigateDestinations.Transaction -> {
                            navigator?.push(
                                TransactionScreen(
                                    greenWallet = viewModel.greenWallet,
                                    transaction = destination.transaction,
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

                        is NavigateDestinations.AccountExchange -> {
                            navigator?.push(
                                AccountExchangeScreen(
                                    greenWallet = viewModel.greenWallet,
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

                        is NavigateDestinations.Send -> {
                            navigator?.push(
                                SendScreen(
                                    greenWallet = viewModel.greenWallet,
                                    address = destination.address,
                                    addressInputType = destination.addressType
                                )
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

                        is NavigateDestinations.Addresses -> {
                            navigator?.push(
                                AddressesScreen(
                                    greenWallet = viewModel.greenWallet,
                                    accountAsset = destination.accountAsset
                                )
                            )
                        }

                        is NavigateDestinations.RecoveryPhrase -> {
                            with(destination.args) {
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

                        is NavigateDestinations.SignMessage -> {
                            bottomSheetNavigator.show(
                                SignMessageBottomSheet(
                                    greenWallet = viewModel.greenWallet,
                                    accountAsset = destination.accountAsset,
                                    address = destination.address
                                )
                            )
                        }

                        is NavigateDestinations.Camera -> {
                            bottomSheetNavigator.show(
                                CameraBottomSheet(
                                    isDecodeContinuous = destination.isDecodeContinuous,
                                    parentScreenName = destination.parentScreenName,
                                    setupArgs = destination.setupArgs
                                )
                            )
                        }

                        is NavigateDestinations.VerifyTransaction -> {
                            bottomSheetNavigator.show(
                                VerifyTransactionBottomSheet(
                                    greenWallet = viewModel.greenWallet,
                                    transactionConfirmLook = destination.transactionConfirmLook
                                )
                            )
                        }
                    }
                }
            }
        }.collect()
    }
}