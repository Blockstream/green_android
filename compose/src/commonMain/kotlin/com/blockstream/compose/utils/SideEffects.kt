package com.blockstream.compose.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
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
import blockstream_green.common.generated.resources.id_jade_firmware_is_outdated
import blockstream_green.common.generated.resources.id_skip
import blockstream_green.common.generated.resources.id_success
import blockstream_green.common.generated.resources.id_the_new_firmware_requires_you
import blockstream_green.common.generated.resources.id_try_again
import blockstream_green.common.generated.resources.id_try_again_using_another_2fa
import blockstream_green.common.generated.resources.id_warning
import com.blockstream.common.SupportType
import com.blockstream.common.data.LogoutReason
import com.blockstream.common.data.SupportData
import com.blockstream.common.data.TwoFactorMethod
import com.blockstream.common.data.TwoFactorResolverData
import com.blockstream.common.data.TwoFactorSetupAction
import com.blockstream.common.devices.DeviceBrand
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.handleException
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.extensions.twoFactorMethodsLocalized
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.navigation.PopTo
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.StringHolder
import com.blockstream.common.utils.createNewTicketUrl
import com.blockstream.compose.LocalAppCoroutine
import com.blockstream.compose.LocalBiometricState
import com.blockstream.compose.LocalDialog
import com.blockstream.compose.LocalSnackbar
import com.blockstream.compose.dialogs.SingleChoiceDialog
import com.blockstream.compose.dialogs.TwoFactorCodeDialog
import com.blockstream.compose.extensions.showErrorSnackbar
import com.blockstream.compose.managers.LocalPlatformManager
import com.blockstream.compose.managers.askForBluetoothPermissions
import com.blockstream.compose.navigation.navigate
import com.blockstream.compose.sideeffects.OpenDialogData
import com.blockstream.compose.sideeffects.openBrowser
import com.blockstream.compose.navigation.LocalNavigator
import com.blockstream.ui.sideeffects.SideEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
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
    val navigator = LocalNavigator.current
    val dialog = LocalDialog.current
    val appCoroutine = LocalAppCoroutine.current
    val platformManager = LocalPlatformManager.current
    val scope = rememberCoroutineScope()
    val biometricsState = LocalBiometricState.current

    var askForBluetoothPermissions by remember { mutableStateOf(false) }
    if (askForBluetoothPermissions) {
        askForBluetoothPermissions(viewModel) {
            askForBluetoothPermissions = false
        }
    }

    var twoFactorResolverData by remember { mutableStateOf<TwoFactorResolverData?>(null) }
    twoFactorResolverData?.also { resolverData ->
        resolverData.methods?.also { methods ->
            SingleChoiceDialog(
                title = stringResource(Res.string.id_choose_method_to_authorize_the),
                items = methods.twoFactorMethodsLocalized().map {
                    stringResource(it)
                },
                dialogProperties = DialogProperties(dismissOnClickOutside = false)
            ) { position ->
                viewModel.postEvent(Events.SelectTwoFactorMethod(method = position?.let {
                    methods.getOrNull(
                        it
                    )
                }))
                twoFactorResolverData = null
            }
        }

        resolverData.authHandlerStatus?.also { authHandlerStatus ->
            TwoFactorCodeDialog(authHandlerStatus = authHandlerStatus) { code, isHelp ->

                viewModel.postEvent(Events.ResolveTwoFactorCode(code = code))

                if (isHelp == true) {
                    appCoroutine.launch {
                        dialog.openDialog(
                            OpenDialogData(
                                title = StringHolder.create(Res.string.id_are_you_not_receiving_your_2fa),
                                message = StringHolder.create(Res.string.id_try_again_using_another_2fa),
                                primaryText = getString(if (resolverData.enable2faCallMethod) Res.string.id_enable_2fa_call_method else Res.string.id_try_again),
                                secondaryText = getString(Res.string.id_contact_support),
                                onPrimary = {
                                    if (resolverData.enable2faCallMethod) {
                                        viewModel.postEvent(
                                            NavigateDestinations.TwoFactorSetup(
                                                greenWallet = viewModel.greenWallet,
                                                method = TwoFactorMethod.PHONE,
                                                action = TwoFactorSetupAction.SETUP,
                                                network = resolverData.network!!,
                                                isSmsBackup = true
                                            )
                                        )
                                    }
                                },
                                onSecondary = {
                                    resolverData.network?.also { network: Network ->
                                        viewModel.postEvent(
                                            NavigateDestinations.Support(
                                                type = SupportType.INCIDENT,
                                                supportData = SupportData.create(
                                                    subject = "I am not receiving my 2FA code",
                                                    network = resolverData.network,
                                                    session = viewModel.sessionOrNull
                                                ),
                                                greenWalletOrNull = viewModel.greenWalletOrNull
                                            )
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

    LaunchedEffect(Unit) {
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
                            type = it.type
                        )
                    }
                }

                is SideEffects.RequestBiometricsCipher -> {
                    biometricsState?.getBiometricsCipher(viewModel)
                }

                is SideEffects.OpenFeeBottomSheet -> {
                    // Pass params to GdkSession
                    viewModel.sessionOrNull?.pendingTransactionParams = it.params

                    viewModel.postEvent(
                        NavigateDestinations.FeeRate(
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

                is SideEffects.ErrorSnackbar -> {
                    appCoroutine.launch {
                        snackbar.showErrorSnackbar(
                            platformManager = platformManager,
                            dialogState = dialog,
                            viewModel = viewModel,
                            error = it.error,
                            supportData = it.supportData
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

                is SideEffects.TwoFactorResolver -> {
                    twoFactorResolverData = it.data
                }

                is SideEffects.ErrorDialog -> {
                    appCoroutine.launch {
                        dialog.openErrorDialog(
                            throwable = it.error,
                            supportData = it.supportData,
                            onErrorReport = { errorReport ->
                                viewModel.postEvent(
                                    NavigateDestinations.Support(
                                        type = SupportType.INCIDENT,
                                        supportData = errorReport,
                                        greenWalletOrNull = viewModel.greenWalletOrNull
                                    )
                                )
                            }
                        )
                    }
                }

                is SideEffects.Share -> {
                    it.text?.also {
                        platformManager.shareText(it)
                    }
                }

                is SideEffects.ShareFile -> {
                    platformManager.shareFile(path = it.path?.toString(), file = it.file)
                }

                is SideEffects.NavigateBack -> {
                    // Check if Navigator exists, else is handled by AppFragment for now
                    val error = it.error
                    if (error != null) {
                        appCoroutine.launch {
                            dialog.openErrorDialog(
                                throwable = error,
                                supportData = it.supportData,
                                onErrorReport = { errorReport ->
                                    viewModel.postEvent(
                                        NavigateDestinations.Support(
                                            type = SupportType.INCIDENT,
                                            supportData = errorReport,
                                            greenWalletOrNull = viewModel.greenWalletOrNull
                                        )
                                    )
                                    navigator.navigateUp()
                                }
                            ) {
                                navigator.navigateUp()
                            }
                        }
                    } else if (it.message != null) {
                        appCoroutine.launch {
                            dialog.openDialog(
                                OpenDialogData(
                                    title = it.title,
                                    message = it.message,
                                    onPrimary = {
                                        navigator.navigateUp()
                                    }
                                )
                            )
                        }
                    } else {
                        navigator.navigateUp()
                    }
                }

                is SideEffects.CopyToClipboard -> {
                    if (!platformManager.copyToClipboard(content = it.value)) {
                        it.message?.also {
                            if (!platformManager.openToast(it)) {
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
                        (if (greenWallet.isEphemeral || greenWallet.isHardware || it.reason == LogoutReason.USER_ACTION) {
                            NavigateDestinations.Home
                        } else {
                            NavigateDestinations.Login(
                                greenWallet = greenWallet,
                                autoLoginWallet = true
                            )
                        }).also { destination ->
                            navigator.navigate(destination) {
                                popUpTo(navigator.graph.id) {
                                    inclusive = false
                                }
                            }
                        }
                    } ?: run {
                        navigator.navigate(NavigateDestinations.Home) {
                            popUpTo(navigator.graph.id) {
                                inclusive = false
                            }
                        }
                    }
                }

                is SideEffects.NavigateAfterSendTransaction -> {
                    viewModel.greenWalletOrNull?.also { greenWallet ->
                        while (navigator.currentBackStack.value.size > 2 && navigator.currentBackStackEntry?.destination?.hasRoute(
                                NavigateDestinations.Transact::class
                            ) != true
                        ) {
                            navigator.navigateUp()
                        }
                    }
                }

                is SideEffects.NavigateToRoot -> {
                    when (it.popTo) {
                        PopTo.Receive -> {
                            navigator.currentBackStack.value.firstOrNull { entry ->
                                entry.destination.hasRoute<NavigateDestinations.Receive>()
                            }?.toRoute<NavigateDestinations.Receive>()?.also { route ->
                                navigator.popBackStack(route, inclusive = false)
                            }
                        }

                        PopTo.Transact -> {
                            navigator.currentBackStack.value.firstOrNull { entry ->
                                entry.destination.hasRoute<NavigateDestinations.Transact>()
                            }?.toRoute<NavigateDestinations.Transact>()?.also { route ->
                                navigator.popBackStack(route, inclusive = false)
                            }
                        }

                        PopTo.OnOffRamps -> {
                            navigator.currentBackStack.value.firstOrNull { entry ->
                                entry.destination.hasRoute<NavigateDestinations.OnOffRamps>()
                            }?.toRoute<NavigateDestinations.OnOffRamps>()?.also { route ->
                                navigator.popBackStack(route, inclusive = false)
                            }
                        }

                        PopTo.Root, null -> {
                            while (navigator.currentBackStack.value.size > 2) {
                                navigator.navigateUp()
                            }
                        }
                    }
                }

                is SideEffects.TransactionSent -> {
                    if (it.data.hasMessageOrUrl) {
                        val isUrl = it.data.url.isNotBlank()

                        dialog.openDialog(
                            OpenDialogData(
                                title = StringHolder.create(Res.string.id_success),
                                message = StringHolder(
                                    string = getString(
                                        Res.string.id_message_from_recipient_s,
                                        it.data.message ?: ""
                                    )
                                ),
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
                                    navigator.popBackStack(navigator.graph.id, inclusive = false)
                                },
                                onSecondary = {
                                    navigator.popBackStack(navigator.graph.id, inclusive = false)
                                },
                                onDismiss = {
                                    navigator.popBackStack(navigator.graph.id, inclusive = false)
                                }
                            ))
                    } else {
                        navigator.popBackStack(navigator.graph.id, inclusive = false)
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

                is SideEffects.RequestDeviceInteraction -> {
                    navigator.navigate(
                        NavigateDestinations.DeviceInteraction(
                            deviceId = it.deviceId,
                            isMasterBlindingKeyRequest = it.isMasterBlindingKeyRequest,
                            message = it.message
                        )
                    )

                    scope.launch(context = handleException()) {
                        it.completable?.also { completable ->
                            completable.await()
                        } ?: run { delay(3000L) }

                        if (navigator.currentBackStackEntry?.destination?.hasRoute(
                                NavigateDestinations.DeviceInteraction::class
                            ) == true
                        ) {
                            navigator.navigateUp()
                        }
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

                is SideEffects.AskForBluetoothPermissions -> {
                    askForBluetoothPermissions = true
                }

                is SideEffects.AskForFirmwareUpgrade -> {

                    if (viewModel.deviceOrNull?.deviceBrand == DeviceBrand.Blockstream) {

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
                            dialog.openDialog(
                                OpenDialogData(
                                    title = StringHolder.create(title),
                                    message = message?.let { StringHolder.create(it) },
                                    items = it.request.firmwareList,
                                    primaryText = getString(Res.string.id_continue),
                                    secondaryText = if (it.request.firmwareList == null) getString(
                                        if (it.request.isUpgradeRequired) Res.string.id_cancel else Res.string.id_skip
                                    ) else null,
                                    onPrimary = {
                                        viewModel.postEvent(Events.RespondToFirmwareUpgrade(index = 0))
                                    },
                                    onSecondary = {
                                        viewModel.postEvent(Events.RespondToFirmwareUpgrade(index = null))
                                    }, onDismiss = {
                                        viewModel.postEvent(Events.RespondToFirmwareUpgrade(index = null))
                                    }, onItem = {
                                        if (it != null) {
                                            viewModel.postEvent(
                                                Events.RespondToFirmwareUpgrade(
                                                    index = it
                                                )
                                            )
                                        }
                                    }
                                ))
                        }

                    } else if (viewModel.deviceOrNull != null && it.request.isUpgradeRequired) {
                        appCoroutine.launch {
                            dialog.openDialog(
                                OpenDialogData(
                                    title = StringHolder.create(Res.string.id_warning),
                                    message = StringHolder.create(Res.string.id_jade_firmware_is_outdated),
                                    primaryText = getString(Res.string.id_continue),
                                    secondaryText = getString(Res.string.id_cancel),
                                    onPrimary = {
                                        viewModel.postEvent(Events.RespondToFirmwareUpgrade(index = 0))
                                    },
                                    onSecondary = {
                                        viewModel.postEvent(Events.RespondToFirmwareUpgrade(index = 0))
                                    }
                                ))
                        }
                    }
                }

                is SideEffects.NavigateTo -> {

                    val navigateTo = when (val destination = it.destination) {
                        is NavigateDestinations.Support -> {
                            if (!viewModel.zendeskSdk.isAvailable) {
                                createNewTicketUrl(
                                    appInfo = viewModel.appInfo,
                                    subject = destination.supportData.subject
                                        ?: viewModel.screenName()
                                            ?.let { "Android Issue in $it" }
                                        ?: "Android Error Report",
                                    supportData = destination.supportData,
                                ).also {
                                    appCoroutine.launch {
                                        openBrowser(
                                            platformManager = platformManager,
                                            dialogState = dialog,
                                            isTor = viewModel.settingsManager.appSettings.tor,
                                            url = it
                                        )
                                    }
                                }
                                false
                            } else {
                                true
                            }
                        }

                        else -> true
                    }

                    if (navigateTo) {
                        navigate(navigator, it.destination)
                    }
                }
            }
        }.collect()
    }
}