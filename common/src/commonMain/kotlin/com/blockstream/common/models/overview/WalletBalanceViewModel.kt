package com.blockstream.common.models.overview

import com.blockstream.common.data.Denomination
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.launchIn
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

interface IWalletBalance : IPostEvent {
    val hideAmounts: StateFlow<Boolean>
    val balancePrimary: StateFlow<String?>
    // val balanceSecondary: StateFlow<String?>
}

open class WalletBalanceViewModel(greenWallet: GreenWallet) :
    GreenViewModel(greenWalletOrNull = greenWallet), IWalletBalance {

    override val hideAmounts: StateFlow<Boolean> = settingsManager.appSettingsStateFlow.map {
        it.hideAmounts
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        settingsManager.appSettings.hideAmounts
    )

    private val _balancePrimary: MutableStateFlow<String?> = MutableStateFlow(null)
    override val balancePrimary: StateFlow<String?> = _balancePrimary.asStateFlow()

    // private val _balanceSecondary: MutableStateFlow<String?> = MutableStateFlow(null)
    // override val balanceSecondary: StateFlow<String?> = _balanceSecondary.asStateFlow()

    class LocalEvents {
        object ToggleBalance : Event
        object ToggleHideAmounts : Event
    }

    override fun bootstrap() {
        super.bootstrap()

        combine(
            session.walletTotalBalance,
            session.walletTotalBalanceDenominationSharedFlow,
            hideAmounts,
            session.settings()
        ) { _, _, _, _ ->
            session.isConnected
        }.filter { isConnected ->
            // Prevent from updating on non connected sessions
            isConnected
        }.onEach {
            updateBalance(
                session.walletTotalBalance.value,
                session.walletTotalBalanceDenominationSharedFlow.value
            )
        }.launchIn(this)
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is LocalEvents.ToggleBalance -> {
                session.walletTotalBalanceDenominationSharedFlow.value =
                    session.walletTotalBalanceDenominationSharedFlow.value.let {
                        if (it == Denomination.BTC) {
                            Denomination.fiat(session)!!
                        } else {
                            Denomination.BTC
                        }
                    }
            }

            is LocalEvents.ToggleHideAmounts -> {
                settingsManager.saveApplicationSettings(
                    settingsManager.getApplicationSettings().let {
                        it.copy(hideAmounts = !it.hideAmounts)
                    })

                if (settingsManager.appSettings.hideAmounts) {
                    countly.hideAmount(session)
                }
            }
        }
    }

    private suspend fun updateBalance(value: Long, denomination: Denomination) {
        // Loading
        if (value == -1L) {
            _balancePrimary.value = null
        } else {
            val balance = session.starsOrNull ?: value.toAmountLook(
                session = session,
                assetId = session.walletAssets.value.data()?.policyId
                    ?: session.defaultNetwork.policyAsset,
                denomination = denomination.takeIf { !it.isFiat }
                    ?: Denomination.fiat(session) // Always create fiat from session, so that we get the correct fiat denomination
            )

            _balancePrimary.value = balance
        }
    }
}