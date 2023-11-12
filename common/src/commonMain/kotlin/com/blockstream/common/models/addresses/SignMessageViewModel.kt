package com.blockstream.common.models.addresses

import com.blockstream.common.data.GreenWallet
import com.blockstream.common.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.device.DeviceResolver
import com.blockstream.common.gdk.params.SignMessageParams
import com.blockstream.common.models.GreenViewModel
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


abstract class SignMessageViewModelAbstract(greenWallet: GreenWallet, account: Account) :
    GreenViewModel(greenWalletOrNull = greenWallet, accountAssetOrNull = account.accountAsset()) {
    override fun screenName(): String = "SignMessage"

    @NativeCoroutinesState
    abstract val message: MutableStateFlow<String>

    @NativeCoroutinesState
    abstract val signature: StateFlow<String?>
}

class SignMessageViewModel(greenWallet: GreenWallet, account: Account, val address: String) :
    SignMessageViewModelAbstract(greenWallet = greenWallet, account = account) {

    override val message: MutableStateFlow<String> = MutableStateFlow("")
    private val _signature: MutableStateFlow<String?> = MutableStateFlow(null)
    override val signature: StateFlow<String?> = _signature.asStateFlow()

    override fun handleEvent(event: Event) {
        super.handleEvent(event)

        if(event is Events.Continue){
            signMessage()
        }
    }

    init {
        bootstrap()
    }

    private fun signMessage(){
        doAsync({
            session.signMessage(
                network = account.network,
                params = SignMessageParams(
                    address = address,
                    message = message.value
                ),
                hardwareWalletResolver = DeviceResolver.createIfNeeded(
                    session.gdkHwWallet,
                    this
                )
            ).signature
        }, onSuccess = {
            _signature.value = it
        })
    }
}
