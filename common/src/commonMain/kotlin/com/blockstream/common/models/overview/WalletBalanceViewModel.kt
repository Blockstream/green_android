package com.blockstream.common.models.overview

import com.blockstream.common.data.Denomination
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.extensions.toggle
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.utils.toAmountLook
import com.blockstream.ui.events.Event
import com.blockstream.ui.models.IPostEvent
import com.rickclephas.kmp.observableviewmodel.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

interface IWalletBalance: IPostEvent {
    val hideAmounts: StateFlow<Boolean>
    val balancePrimary: StateFlow<String?>
    val balanceSecondary: StateFlow<String?>
}


open class WalletBalanceViewModel(greenWallet: GreenWallet): GreenViewModel(greenWalletOrNull = greenWallet), IWalletBalance {

    override val hideAmounts: StateFlow<Boolean> = settingsManager.appSettingsStateFlow.map {
        it.hideAmounts
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), settingsManager.appSettings.hideAmounts)

    private val _balancePrimary: MutableStateFlow<String?> = MutableStateFlow(null)
    override val balancePrimary: StateFlow<String?> = _balancePrimary.asStateFlow()

    private val _balanceSecondary: MutableStateFlow<String?> = MutableStateFlow(null)
    override val balanceSecondary: StateFlow<String?> = _balanceSecondary.asStateFlow()

    private val primaryBalanceInFiat: MutableStateFlow<Boolean> = MutableStateFlow(false)


    class LocalEvents {
        object ToggleBalance : Event
    }

    override fun bootstrap() {
        super.bootstrap()

        combine(session.walletTotalBalance, primaryBalanceInFiat, hideAmounts, session.settings()) { _, _, _, _ ->
            session.isConnected
        }.filter { isConnected ->
            // Prevent from updating on non connected sessions
            isConnected
        }.onEach {
            updateBalance()
        }.launchIn(this)
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is LocalEvents.ToggleBalance -> {
                primaryBalanceInFiat.toggle()
            }
        }
    }

    private suspend fun updateBalance() {
        // Loading
        if (session.walletTotalBalance.value == -1L) {
            _balancePrimary.value = null
            _balanceSecondary.value = null
        } else {
            val balance = session.starsOrNull ?: session.walletTotalBalance.value.toAmountLook(
                session = session,
                assetId = session.walletAssets.value.policyId
            )

            val fiat = session.starsOrNull ?: session.walletTotalBalance.value.toAmountLook(
                session = session,
                denomination = Denomination.fiat(session)
            )

            if (primaryBalanceInFiat.value) {
                _balancePrimary.value = fiat
                _balanceSecondary.value = balance
            } else {
                _balancePrimary.value = balance
                _balanceSecondary.value = fiat
            }
        }
    }
}