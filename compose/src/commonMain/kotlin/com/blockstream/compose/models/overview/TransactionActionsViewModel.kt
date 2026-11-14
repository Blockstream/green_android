package com.blockstream.compose.models.overview

import androidx.lifecycle.viewModelScope
import com.blockstream.compose.events.Events
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.extensions.launchSafe
import com.blockstream.domain.swap.IsSwapAvailableUseCase
import com.blockstream.domain.swap.IsSwapsEnabledUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.inject

abstract class TransactionActionsViewModel(greenWallet: GreenWallet) : WalletBalanceViewModel(greenWallet = greenWallet) {

    private val isSwapAvailableUseCase: IsSwapAvailableUseCase by inject()
    private val isSwapsEnabledUseCase: IsSwapsEnabledUseCase by inject()

    open val isSwapAvailable: StateFlow<Boolean> = sessionOrNull?.takeIf { it.isConnected }?.let { session ->
        session.accountAsset.map {
            isSwapAvailableUseCase(wallet = greenWallet, session = session)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            isSwapAvailableUseCase(wallet = greenWallet, session = session)
        )
    } ?: MutableStateFlow(false)

    fun onBuy() {
        postEvent(NavigateDestinations.Buy(greenWallet = greenWallet))
    }

    fun onSend() {
        postEvent(NavigateDestinations.SendAddress(greenWallet = greenWallet))
    }

    fun onReceive() {
        postEvent(NavigateDestinations.ReceiveChooseAsset(greenWallet = greenWallet))
    }

    fun onSwap() {
        postEvent(Events.SwapEntry)
        viewModelScope.launchSafe {
            if (isSwapsEnabledUseCase(greenWallet)) {
                postEvent(NavigateDestinations.Swap(greenWallet = greenWallet, accountAsset = accountAsset.value))
            } else {
                postEvent(NavigateDestinations.EnableJadeFeature(greenWallet = greenWallet, accountAsset = accountAsset.value))
            }
        }
    }
}
