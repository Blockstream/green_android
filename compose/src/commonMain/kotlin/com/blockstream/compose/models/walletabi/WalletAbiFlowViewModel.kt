package com.blockstream.compose.models.walletabi

import androidx.lifecycle.viewModelScope
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.domain.walletabi.flow.WalletAbiFlowIntent
import com.blockstream.domain.walletabi.flow.WalletAbiFlowState
import com.blockstream.domain.walletabi.flow.WalletAbiFlowStore
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WalletAbiFlowViewModel(
    private val store: WalletAbiFlowStore
) : GreenViewModel() {
    val state: StateFlow<WalletAbiFlowState> = store.state

    fun dispatch(intent: WalletAbiFlowIntent) {
        viewModelScope.launch {
            store.dispatch(intent)
        }
    }
}
