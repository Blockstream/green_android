package com.blockstream.common.models.support

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_contact_support
import blockstream_green.common.generated.resources.id_thank_you_for_your_feedback
import blockstream_green.common.generated.resources.id_thanks_your_message_has_been_sent
import com.blockstream.common.SupportType
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.NavData
import com.blockstream.common.data.SupportData
import com.blockstream.common.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.Loggable
import com.blockstream.common.utils.StringHolder
import com.blockstream.common.utils.isEmailValid
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import org.jetbrains.compose.resources.getString

abstract class SupportViewModelAbstract(
    val type: SupportType,
    val supportData: SupportData,
    greenWalletOrNull: GreenWallet?
) : GreenViewModel(
    greenWalletOrNull = greenWalletOrNull
) {
    override fun screenName(): String = "Support"

    override val isLoginRequired: Boolean = false

    @NativeCoroutinesState
    abstract val email: MutableStateFlow<String>

    @NativeCoroutinesState
    abstract val message: MutableStateFlow<String>

    @NativeCoroutinesState
    abstract val attachLogs: MutableStateFlow<Boolean>

    @NativeCoroutinesState
    abstract val isTorEnabled: MutableStateFlow<Boolean>

    @NativeCoroutinesState
    abstract val torAcknowledged: MutableStateFlow<Boolean>
}

class SupportViewModel(type: SupportType, supportData: SupportData, greenWalletOrNull: GreenWallet?) :
    SupportViewModelAbstract(type = type, supportData = supportData, greenWalletOrNull = greenWalletOrNull) {

    override val email: MutableStateFlow<String> = MutableStateFlow("")
    override val message: MutableStateFlow<String> = MutableStateFlow("")
    override val attachLogs: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val isTorEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val torAcknowledged: MutableStateFlow<Boolean> = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            _navData.value = NavData(
                title = getString(Res.string.id_contact_support)
            )
        }

        settingsManager.appSettingsStateFlow.onEach {
            isTorEnabled.value = it.tor
        }.launchIn(this)

        combine(email, message, isTorEnabled, torAcknowledged) { email, message, isTorEnabled, torAcknowledged ->
            _isValid.value = email.isEmailValid() && message.isNotBlank() && (!isTorEnabled || torAcknowledged)
        }.launchIn(this)

        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        if (event is Events.Continue) {
            submitContactRequest()
        }
    }

    private fun submitContactRequest() {
        doAsync({
            val subject = (if (type == SupportType.FEEDBACK) "Feedback from" else "Bug report from").let { "$it ${appInfo.userAgent} ${supportData.subject ?: ""}".trim() }
            zendeskSdk.submitNewTicket(
                type = type,
                subject = subject,
                email = email.value,
                message = message.value,
                supportData = if(attachLogs.value) supportData.withGdkLogs(sessionOrNull) else supportData,
                autoRetry = false
            )
        }, onSuccess = {
            postSideEffect(SideEffects.Snackbar(text = StringHolder.create(if (type == SupportType.FEEDBACK) Res.string.id_thank_you_for_your_feedback else Res.string.id_thanks_your_message_has_been_sent)))
            postSideEffect(SideEffects.NavigateBack())
        })
    }

    companion object : Loggable()
}

class SupportViewModelPreview() : SupportViewModelAbstract(
    type = SupportType.FEEDBACK, supportData = SupportData.create(), greenWalletOrNull = previewWallet()
) {
    override val email: MutableStateFlow<String> = MutableStateFlow("")
    override val message: MutableStateFlow<String> = MutableStateFlow("")
    override val attachLogs: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val isTorEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val torAcknowledged: MutableStateFlow<Boolean> = MutableStateFlow(false)

    companion object {
        fun preview() = SupportViewModelPreview()
    }
}