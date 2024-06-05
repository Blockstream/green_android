package com.blockstream.common.models.settings

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_1s_twofactor_setup
import blockstream_green.common.generated.resources.id_2fa_call_is_now_enabled
import blockstream_green.common.generated.resources.id_cancel_2fa_reset
import blockstream_green.common.generated.resources.id_continue
import blockstream_green.common.generated.resources.id_dispute_twofactor_reset
import blockstream_green.common.generated.resources.id_if_you_did_not_request_the
import blockstream_green.common.generated.resources.id_if_you_initiated_the_2fa_reset
import blockstream_green.common.generated.resources.id_insert_your_email_to_receive
import blockstream_green.common.generated.resources.id_insert_your_phone_number_to
import blockstream_green.common.generated.resources.id_request_twofactor_reset
import blockstream_green.common.generated.resources.id_resetting_your_twofactor_takes
import blockstream_green.common.generated.resources.id_undo_2fa_dispute
import blockstream_green.common.generated.resources.id_use_your_email_to_receive
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
import com.blockstream.common.utils.StringHolder
import com.blockstream.common.utils.isEmailValid
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import org.jetbrains.compose.resources.getString

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
    abstract val messageText: StateFlow<String?>

    @NativeCoroutinesState
    abstract val actionText: StateFlow<String?>

    @NativeCoroutinesState
    abstract val country: MutableStateFlow<String>

    @NativeCoroutinesState
    abstract val number: MutableStateFlow<String>

    @NativeCoroutinesState
    abstract val email: MutableStateFlow<String>

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

    private val _messageText: MutableStateFlow<String?> = MutableStateFlow(null)
    override val messageText: StateFlow<String?> = _messageText.asStateFlow()

    private val _actionText: MutableStateFlow<String?> = MutableStateFlow(null)
    override val actionText: StateFlow<String?> = _actionText.asStateFlow()

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

        viewModelScope.launch {
            val title = when (action) {
                TwoFactorSetupAction.SETUP, TwoFactorSetupAction.SETUP_EMAIL -> {
                    getString(Res.string.id_1s_twofactor_setup, getString(method.localized))
                }

                TwoFactorSetupAction.RESET -> {
                    getString(Res.string.id_request_twofactor_reset)
                }

                TwoFactorSetupAction.CANCEL -> {
                    getString(Res.string.id_cancel_2fa_reset)
                }

                TwoFactorSetupAction.DISPUTE -> {
                    getString(Res.string.id_dispute_twofactor_reset)
                }

                TwoFactorSetupAction.UNDO_DISPUTE -> {
                    getString(Res.string.id_undo_2fa_dispute)
                }
            }

            when(action){
                TwoFactorSetupAction.SETUP -> {
                    _messageText.value =  if(method == TwoFactorMethod.EMAIL){
                        getString(Res.string.id_insert_your_email_to_receive)
                    }else if(method != TwoFactorMethod.AUTHENTICATOR){
                        getString(Res.string.id_insert_your_phone_number_to)
                    } else null
                    _actionText.value = getString(Res.string.id_continue)
                }
                TwoFactorSetupAction.SETUP_EMAIL -> {
                    _messageText.value = getString(Res.string.id_use_your_email_to_receive)
                    _actionText.value = getString(Res.string.id_continue)
                }
                TwoFactorSetupAction.RESET -> {
                    _messageText.value = getString(Res.string.id_resetting_your_twofactor_takes)
                    _actionText.value = getString(Res.string.id_request_twofactor_reset)
                }
                TwoFactorSetupAction.DISPUTE -> {
                    _messageText.value = getString(Res.string.id_if_you_did_not_request_the)
                    _actionText.value = getString(Res.string.id_dispute_twofactor_reset)
                }
                TwoFactorSetupAction.UNDO_DISPUTE -> {
                    _messageText.value = getString(Res.string.id_if_you_initiated_the_2fa_reset)
                    _actionText.value = getString(Res.string.id_undo_2fa_dispute)
                }
                TwoFactorSetupAction.CANCEL -> {
                    // Cancel action init
                }
            }

            _navData.value = NavData(title = title, subtitle = greenWallet.name)
        }

        session.ifConnected {

            if(action == TwoFactorSetupAction.CANCEL){
                cancel2FA()
            }

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

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {

            is LocalEvents.Cancel2FA -> {
                cancel2FA()
            }

            is Events.Continue -> {
                when(action){
                    TwoFactorSetupAction.RESET, TwoFactorSetupAction.DISPUTE -> {
                        reset2FA()
                    }

                    TwoFactorSetupAction.UNDO_DISPUTE -> {
                        undoReset2FA()
                    }
                    TwoFactorSetupAction.SETUP,TwoFactorSetupAction.SETUP_EMAIL  -> {
                        enable2FA()
                    }
                    TwoFactorSetupAction.CANCEL -> {
                        // Cancel is triggered immediately
                    }
                }
            }
        }
    }

    private fun enable2FA() {
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
                    twoFactorResolver = this
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
                postSideEffect(SideEffects.Snackbar(StringHolder.create(Res.string.id_2fa_call_is_now_enabled)))
            }
            postSideEffect(SideEffects.NavigateBack())
        })
    }

    private fun reset2FA() {
        doAsync({
            session
                .twoFactorReset(
                    network = network,
                    email = email.value,
                    isDispute = action == TwoFactorSetupAction.DISPUTE,
                    twoFactorResolver = this
                )
        }, onSuccess = {
            postSideEffect(SideEffects.NavigateBack())
        })
    }

    private fun undoReset2FA() {
        doAsync({
            session
                .twoFactorUndoReset(
                    network = network,
                    email = email.value,
                    twoFactorResolver = this
                )
        }, onSuccess = {
            postSideEffect(SideEffects.NavigateBack())
        })
    }

    private fun cancel2FA() {
        doAsync({
            session.twoFactorCancelReset(network = network, twoFactorResolver = this)
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
        method = TwoFactorMethod.AUTHENTICATOR,
        action = TwoFactorSetupAction.SETUP
    ) {
    override val messageText: StateFlow<String?> = MutableStateFlow(null)
    override val actionText: StateFlow<String?> = MutableStateFlow("Continue")

    override val country: MutableStateFlow<String> = MutableStateFlow("+237")
    override val number: MutableStateFlow<String> = MutableStateFlow("")
    override val email: MutableStateFlow<String> = MutableStateFlow("")
    override val qr: StateFlow<String?> = MutableStateFlow("This is a QR")
    override val authenticatorCode: StateFlow<String?> = MutableStateFlow("This is the code")

    companion object {
        fun preview() = TwoFactorSetupViewModelPreview(previewWallet(isHardware = false))
        fun previewRecovery() = TwoFactorSetupViewModelPreview(
            previewWallet(isHardware = false),
        )
    }
}