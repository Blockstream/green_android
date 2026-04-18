package com.blockstream.compose.models.walletabi

import androidx.lifecycle.viewModelScope
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.navigation.NavData
import com.blockstream.data.data.GreenWallet
import com.blockstream.domain.walletabi.flow.WalletAbiFlowIntent
import com.blockstream.domain.walletabi.flow.WalletAbiFlowState
import com.blockstream.domain.walletabi.flow.WalletAbiFlowStore
import com.blockstream.domain.walletabi.flow.WalletAbiStartRequestContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WalletAbiFlowRouteViewModel(
    greenWallet: GreenWallet,
    private val store: WalletAbiFlowStore
) : GreenViewModel(greenWalletOrNull = greenWallet) {
    override fun screenName(): String = "WalletAbiFlow"
    override val isLoginRequired: Boolean = false

    val state: StateFlow<WalletAbiFlowState> = store.state

    init {
        _navData.value = NavData(
            title = "Wallet ABI",
            subtitle = greenWallet.name
        )
        startFlow(greenWallet = greenWallet)
        bootstrap()
    }

    fun dispatch(intent: WalletAbiFlowIntent) {
        viewModelScope.launch {
            store.dispatch(intent)
        }
    }

    private fun startFlow(greenWallet: GreenWallet) {
        dispatch(
            WalletAbiFlowIntent.Start(
                requestContext = WalletAbiStartRequestContext(
                    requestId = DEMO_REQUEST_ID,
                    walletId = greenWallet.id
                )
            )
        )
    }

    companion object {
        const val DEMO_REQUEST_ID = "wallet-abi-demo-request"
    }
}
