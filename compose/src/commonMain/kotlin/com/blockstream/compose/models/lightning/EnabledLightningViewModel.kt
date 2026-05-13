package com.blockstream.compose.models.lightning

import androidx.lifecycle.viewModelScope
import com.blockstream.compose.events.Event
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.data.data.GreenWallet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

abstract class EnabledLightningViewModelAbstract(greenWallet: GreenWallet) :
    GreenViewModel(greenWalletOrNull = greenWallet) {

    abstract val nodeId: StateFlow<String?>
}

class EnabledLightningViewModel(greenWallet: GreenWallet) :
    EnabledLightningViewModelAbstract(greenWallet = greenWallet) {
    override fun screenName(): String = "EnabledLightning"

    private val _nodeId = MutableStateFlow<String?>(null)
    override val nodeId = _nodeId.asStateFlow()

    class LocalEvents {
        object CopyNodeId : Event
    }

    init {
        viewModelScope.launch {
            session.lightningSdkOrNull?.nodeInfoStateFlow
                ?.catch { cause ->
                    cause.printStackTrace()
                }
                ?.collect { nodeInfo ->
                _nodeId.value = nodeInfo.id
            }
        }

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