package com.blockstream.compose.models.demo

import androidx.lifecycle.viewModelScope
import com.blockstream.common.Urls
import com.blockstream.common.data.DataState
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.utils.toAmountLookOrNa
import com.blockstream.compose.events.Event
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.sideeffects.SideEffect
import com.blockstream.compose.sideeffects.SideEffects
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DemoViewModel : GreenViewModel() {
    val counter = MutableStateFlow(0)

    val data = MutableStateFlow<DataState<Int>>(DataState.Loading)

    class LocalEvents {
        object EventOpenBrowser : Event
        object EventLogin : Event
        object EventRefresh : Event
        object SideEffectOne : SideEffect
        object SideEffectTwo : SideEffect
    }

    override suspend fun handleEvent(event: Event) {
        when (event) {
            is LocalEvents.EventOpenBrowser -> {
                postSideEffect(SideEffects.OpenBrowser(Urls.BLOCKSTREAM_GREEN_WEBSITE))
            }

            is LocalEvents.EventRefresh -> {
                gdkSession.updateAccountsAndBalances(refresh = true)
                gdkSession.updateWalletTransactions()
            }
        }
    }

    val gdkSession: GdkSession

    init {
        viewModelScope.launch {
            while (true) {
                delay(3000L)
                data.value = DataState.Success(1337)
                delay(3000L)
                data.value = DataState.Loading
                delay(3000L)
                data.value = DataState.Empty
                delay(3000L)
                data.value = DataState.Error(Exception("OMG an error"))
            }
        }

        this.gdkSession = sessionManager.getOnBoardingSession()
        bootstrap()
    }

    val accounts get() = gdkSession.accounts

    val transactions get() = gdkSession.walletTransactions

    val walletAssets get() = gdkSession.walletAssets

    val walletBalance = gdkSession.walletTotalBalance
        .map {
            if (it > -1) {
                it.toAmountLookOrNa(
                    session = gdkSession,
                    withUnit = true
                )
            } else {
                "-"
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "Disconnected")
}
