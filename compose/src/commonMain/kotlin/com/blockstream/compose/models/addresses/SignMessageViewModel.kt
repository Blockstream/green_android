package com.blockstream.compose.models.addresses

import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.ifConnected
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.extensions.previewAccountAsset
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.device.DeviceResolver
import com.blockstream.common.gdk.params.SignMessageParams
import com.blockstream.compose.events.Event
import com.blockstream.compose.events.Events
import com.blockstream.compose.models.GreenViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach

abstract class SignMessageViewModelAbstract(
    greenWallet: GreenWallet,
    accountAsset: AccountAsset,
    val address: String
) :
    GreenViewModel(greenWalletOrNull = greenWallet, accountAssetOrNull = accountAsset) {
    override fun screenName(): String = "SignMessage"
    abstract val message: MutableStateFlow<String>
    abstract val signature: StateFlow<String?>
}

class SignMessageViewModel(greenWallet: GreenWallet, accountAsset: AccountAsset, address: String) :
    SignMessageViewModelAbstract(
        greenWallet = greenWallet,
        accountAsset = accountAsset,
        address = address
    ) {

    override val message: MutableStateFlow<String> = MutableStateFlow("")
    private val _signature: MutableStateFlow<String?> = MutableStateFlow(null)
    override val signature: StateFlow<String?> = _signature.asStateFlow()

    class LocalEvents {
        object TryAgain : Event
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        if (event is Events.Continue) {
            signMessage()
        } else if (event is LocalEvents.TryAgain) {
            _signature.value = null
        }
    }

    init {
        session.ifConnected {
            message.onEach {
                _isValid.value = it.isNotBlank()
            }.launchIn(this)
        }
        bootstrap()
    }

    private fun signMessage() {
        doAsync({
            session.signMessage(
                network = account.network,
                params = SignMessageParams(
                    address = address,
                    message = message.value
                ),
                hardwareWalletResolver = DeviceResolver.createIfNeeded(session.gdkHwWallet)
            ).signature
        }, onSuccess = {
            _signature.value = it
        })
    }
}

class SignMessageViewModelPreview : SignMessageViewModelAbstract(
    previewWallet(),
    previewAccountAsset(),
    "bc1qaqtq80759n35gk6ftc57vh7du83nwvt5lgkznu"
) {
    override val message: MutableStateFlow<String> = MutableStateFlow("This is my message")
    override val signature: StateFlow<String?> =
        MutableStateFlow<String?>("This is the generated Signature")

    companion object {
        fun preview() = SignMessageViewModelPreview()
    }
}