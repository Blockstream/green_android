package com.blockstream.compose.models.lightning

import com.blockstream.compose.events.Event
import com.blockstream.compose.extensions.launchIn
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.data.data.GreenWallet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach

abstract class EnabledLightningViewModelAbstract(greenWallet: GreenWallet) :
    GreenViewModel(greenWalletOrNull = greenWallet) {

    abstract val nodeId: StateFlow<String?>
}

class EnabledLightningViewModel(greenWallet: GreenWallet) :
    EnabledLightningViewModelAbstract(greenWallet = greenWallet) {
    override fun screenName(): String = "EnabledLightning"

    final override val nodeId: StateFlow<String?>
        field = MutableStateFlow(session.lightningNodeId)

    class LocalEvents {
        object CopyNodeId : Event
    }

    init {
        session.lightningSdkOrNull?.nodeInfoStateFlow?.onEach {
            nodeId.value = it.id
        }?.launchIn(this)

        bootstrap()
    }


    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            LocalEvents.CopyNodeId -> {
                nodeId.value?.let { nodeId ->
                    postSideEffect(SideEffects.CopyToClipboard(nodeId))
                }
            }
        }
    }
}