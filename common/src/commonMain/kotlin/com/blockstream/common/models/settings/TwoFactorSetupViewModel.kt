package com.blockstream.common.models.settings

import com.blockstream.common.Urls
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.NavData
import com.blockstream.common.data.TwoFactorMethod
import com.blockstream.common.data.TwoFactorSetupAction
import com.blockstream.common.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.ifConnected
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.extensions.previewNetwork
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.TwoFactorResolver
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.data.SettingsNotification
import com.blockstream.common.gdk.data.TwoFactorMethodConfig
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.isEmailValid
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach

abstract class TwoFactorSetupViewModelAbstract(
    greenWallet: GreenWallet,
    val network: Network,
    val method: TwoFactorMethod,
    val action: TwoFactorSetupAction,
) :
    GreenViewModel(greenWalletOrNull = greenWallet) {
    override fun screenName(): String = when (action) {
        TwoFactorSetupAction.SETUP, TwoFactorSetupAction.SETUP_EMAIL -> "Setup"
        TwoFactorSetupAction.RESET -> "Reset"
        TwoFactorSetupAction.CANCEL -> "CancelDispute"
        TwoFactorSetupAction.DISPUTE -> "Dispute"
        TwoFactorSetupAction.UNDO_DISPUTE -> "UndoDispute"
    }.let {
        "WalletSettings2FA$it"
    }

    override fun segmentation(): HashMap<String, Any>? = session.ifConnected {
        countly.twoFactorSegmentation(
            session = session,
            network = network,
            twoFactorMethod = method.gdkType
        )
    }

    @NativeCoroutinesState
    abstract val country: StateFlow<String?>

    @NativeCoroutinesState
    abstract val number: StateFlow<String?>

    @NativeCoroutinesState
    abstract val email: StateFlow<String?>

    @NativeCoroutinesState
    abstract val qr: StateFlow<String?>

    @NativeCoroutinesState
    abstract val authenticatorCode: StateFlow<String?>
}

class TwoFactorSetupViewModel(
    greenWallet: GreenWallet,
    network: Network,
    method: TwoFactorMethod,
    action: TwoFactorSetupAction,
    private val isSmsBackup: Boolean = false
) : TwoFactorSetupViewModelAbstract(greenWallet = greenWallet, network = network, method = method, action = action) {

    override val country: MutableStateFlow<String> = MutableStateFlow("")
    override val number: MutableStateFlow<String> = MutableStateFlow("")
    override val email: MutableStateFlow<String> = MutableStateFlow("")

    private val _qr = MutableStateFlow<String?>(null)
    override val qr: StateFlow<String?> = _qr.asStateFlow()

    private val _authenticatorCode = MutableStateFlow<String?>(null)
    override val authenticatorCode: StateFlow<String?> = _authenticatorCode.asStateFlow()

    private var _authenticatorUrl: String? = null

    class LocalEvents {
        data class Enable2FA(val twoFactorResolver: TwoFactorResolver) : Event
        data class Reset2FA(val twoFactorResolver: TwoFactorResolver) : Event
        data class UndoReset2FA(val twoFactorResolver: TwoFactorResolver) : Event
        data class Cancel2FA(val twoFactorResolver: TwoFactorResolver) : Event

        class ClickTermsOfService : Events.OpenBrowser(Urls.TERMS_OF_SERVICE)
        class ClickPrivacyPolicy : Events.OpenBrowser(Urls.PRIVACY_POLICY)
        class ClickHelp : Events.OpenBrowser(Urls.HELP_CENTER)
    }

    init {
        val title = when (action) {
            TwoFactorSetupAction.SETUP, TwoFactorSetupAction.SETUP_EMAIL -> {
                "id_1s_twofactor_setup|${method.localized}"
            }

            TwoFactorSetupAction.RESET -> {
                "id_request_twofactor_reset"
            }

            TwoFactorSetupAction.CANCEL -> {
                "id_cancel_2fa_reset"
            }

            TwoFactorSetupAction.DISPUTE -> {
                "id_dispute_twofactor_reset"
            }

            TwoFactorSetupAction.UNDO_DISPUTE -> {
                "id_undo_2fa_dispute"
            }
        }

        _navData.value = NavData(title = title)

        session.ifConnected {
            doAsync({
                session.getTwoFactorConfig(network)
                    ?: throw Exception("Two Factor Config couldn't resolved")
            }, preAction = null, postAction = null, onSuccess = {
                _authenticatorUrl = it.gauth.data
                _qr.value = it.gauth.data
                _authenticatorCode.value = it.gauth.data.split("=").getOrNull(1)
            }, onError = {
                postSideEffect(SideEffects.NavigateBack(error = it))
            })

            if (method == TwoFactorMethod.PHONE || method == TwoFactorMethod.SMS) {
                combine(country, number) { country, number ->
                    country.isNotBlank() && (number.trim().length) > 7
                }.onEach {
                    _isValid.value = it
                }.launchIn(this)
            } else if (method == TwoFactorMethod.EMAIL) {
                email.onEach {
                    _isValid.value = it.isEmailValid()
                }.launchIn(this)
            } else if (method == TwoFactorMethod.AUTHENTICATOR) {
                _authenticatorCode.onEach {
                    _isValid.value = !it.isNullOrBlank()
                }.launchIn(this)
            }

            null
        }

        bootstrap()
    }

    override fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {

            is LocalEvents.Enable2FA -> {
                enable2FA(event.twoFactorResolver)
            }

            is LocalEvents.Reset2FA -> {
                reset2FA(event.twoFactorResolver)
            }

            is LocalEvents.UndoReset2FA -> {
                undoReset2FA(event.twoFactorResolver)
            }

            is LocalEvents.Cancel2FA -> {
                cancel2FA(event.twoFactorResolver)
            }
        }
    }

    private fun enable2FA(twoFactorResolver: TwoFactorResolver) {
        val data = when(method){
            TwoFactorMethod.SMS, TwoFactorMethod.PHONE, TwoFactorMethod.TELEGRAM -> {
                "${country.value}${number.value}"
            }
            TwoFactorMethod.EMAIL -> {
                email.value
            }
            TwoFactorMethod.AUTHENTICATOR -> {
                _authenticatorUrl ?: ""
            }
        }

        doAsync({
            session
                .changeSettingsTwoFactor(
                    network = network,
                    method = method.gdkType,
                    methodConfig = TwoFactorMethodConfig(
                        confirmed = true,
                        enabled = action != TwoFactorSetupAction.SETUP_EMAIL,
                        data = data,
                        isSmsBackup = isSmsBackup
                    ),
                    twoFactorResolver = twoFactorResolver
                )

            // Enable legacy recovery emails
            if (action == TwoFactorSetupAction.SETUP_EMAIL) {
                session.getSettings(network)?.copy(
                    notifications = SettingsNotification(
                        emailIncoming = true,
                        emailOutgoing = true
                    )
                )?.also { newSettings ->
                    session.changeSettings(network, newSettings)
                    session.updateSettings(network)
                }
            }
        }, onSuccess = {
            if (isSmsBackup) {
                postSideEffect(SideEffects.Snackbar("id_2fa_call_is_now_enabled"))
            }
            postSideEffect(SideEffects.NavigateBack())
        })
    }

    private fun reset2FA(
        twoFactorResolver: TwoFactorResolver
    ) {
        doAsync({
            session
                .twoFactorReset(
                    network = network,
                    email = email.value,
                    isDispute = action == TwoFactorSetupAction.DISPUTE,
                    twoFactorResolver = twoFactorResolver
                )
        }, onSuccess = {
            postSideEffect(SideEffects.NavigateBack())
        })
    }

    private fun undoReset2FA(twoFactorResolver: TwoFactorResolver) {
        doAsync({
            session
                .twoFactorUndoReset(
                    network = network,
                    email = email.value,
                    twoFactorResolver = twoFactorResolver
                )
        }, onSuccess = {
            postSideEffect(SideEffects.NavigateBack())
        })
    }

    private fun cancel2FA(twoFactorResolver: TwoFactorResolver) {
        doAsync({
            session.twoFactorCancelReset(network = network, twoFactorResolver = twoFactorResolver)
        }, onSuccess = {
            postSideEffect(SideEffects.NavigateBack())
        }, onError = {
            postSideEffect(SideEffects.NavigateBack(error = it, errorReport = errorReport(it)))
        })
    }
}

class TwoFactorSetupViewModelPreview(
    greenWallet: GreenWallet,
) :
    TwoFactorSetupViewModelAbstract(
        greenWallet = greenWallet,
        network = previewNetwork(),
        method = TwoFactorMethod.SMS,
        action = TwoFactorSetupAction.SETUP
    ) {

    override val country: StateFlow<String> = MutableStateFlow("")
    override val number: StateFlow<String> = MutableStateFlow("")
    override val email: StateFlow<String> = MutableStateFlow("")
    override val qr: StateFlow<String?> = MutableStateFlow(null)
    override val authenticatorCode: StateFlow<String?> = MutableStateFlow(null)

    companion object {
        fun preview() = TwoFactorSetupViewModelPreview(previewWallet(isHardware = false))
        fun previewRecovery() = TwoFactorSetupViewModelPreview(
            previewWallet(isHardware = false),
        )
    }
}