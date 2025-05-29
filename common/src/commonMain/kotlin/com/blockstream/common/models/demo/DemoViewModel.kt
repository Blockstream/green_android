package com.blockstream.common.models.demo

import com.blockstream.common.Urls
import com.blockstream.common.data.DataState
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.toAmountLookOrNa
import com.blockstream.ui.events.Event
import com.blockstream.ui.sideeffects.SideEffect
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.MutableStateFlow
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import com.rickclephas.kmp.observableviewmodel.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class DemoViewModel : GreenViewModel() {
    @NativeCoroutinesState
    val counter = MutableStateFlow(viewModelScope, 0)

    @NativeCoroutinesState
    val data = MutableStateFlow<DataState<Int>>(viewModelScope, DataState.Loading)

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
        viewModelScope.coroutineScope.launch {
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

    @NativeCoroutinesState
    val accounts get() = gdkSession.accounts

    @NativeCoroutinesState
    val transactions get() = gdkSession.walletTransactions

    @NativeCoroutinesState
    val walletAssets get() = gdkSession.walletAssets

    @NativeCoroutinesState
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
