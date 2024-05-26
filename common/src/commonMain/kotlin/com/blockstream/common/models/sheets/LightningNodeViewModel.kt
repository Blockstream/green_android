package com.blockstream.common.models.sheets

import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.events.Event
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.lightning.LightningManager
import com.blockstream.common.lightning.channelsBalanceSatoshi
import com.blockstream.common.lightning.inboundLiquiditySatoshi
import com.blockstream.common.lightning.maxPayableSatoshi
import com.blockstream.common.lightning.maxReceivableSatoshi
import com.blockstream.common.lightning.maxSinglePaymentAmountSatoshi
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.Loggable
import com.blockstream.common.utils.toAmountLookOrNa
import com.rickclephas.kmm.viewmodel.stateIn
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.inject

abstract class LightningNodeViewModelAbstract(
    greenWallet: GreenWallet
) : GreenViewModel(
    greenWalletOrNull = greenWallet
) {
    @NativeCoroutinesState
    abstract val data: StateFlow<List<Pair<String, String>>>

    @NativeCoroutinesState
    abstract val showEmptyAccount: StateFlow<Boolean>
}

class LightningNodeViewModel(greenWallet: GreenWallet) :
    LightningNodeViewModelAbstract(
        greenWallet = greenWallet
    ) {
    override fun screenName(): String = "LightningNodeState"

    private val _data: MutableStateFlow<List<Pair<String, String>>> = MutableStateFlow(listOf())
    override val data = _data.asStateFlow()

    override val showEmptyAccount = session.lightningSdk.nodeInfoStateFlow.map {
        it.channelsBalanceSatoshi() > 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val lightningManager: LightningManager by inject()

    class LocalEvents {
        object ShowRecoveryPhrase : Event
        object EmptyAccount : Event
        object RescanSwaps : Event
        object ShareLogs : Event
    }

    init {
        if (session.isConnected) {

            session.lightningSdk.nodeInfoStateFlow.onEach {
                val list = mutableListOf(
                    "ID" to it.id,
                    "id_account_balance" to it.channelsBalanceSatoshi().toAmountLookOrNa(
                        session = session,
                        withUnit = true,
                        withGrouping = true
                    ),
                    "id_inbound_liquidity" to it.inboundLiquiditySatoshi().toAmountLookOrNa(
                        session = session,
                        withUnit = true,
                        withGrouping = true
                    ),
                    "id_max_payable_amount" to it.maxPayableSatoshi().toAmountLookOrNa(
                        session = session,
                        withUnit = true,
                        withGrouping = true
                    ),
                    "id_max_single_payment_amount" to it.maxSinglePaymentAmountSatoshi()
                        .toAmountLookOrNa(
                            session = session,
                            withUnit = true,
                            withGrouping = true
                        ),
                    "id_max_receivable_amount" to it.maxReceivableSatoshi().toAmountLookOrNa(
                        session = session,
                        withUnit = true,
                        withGrouping = true
                    )
                )


                if (appInfo.isDevelopmentOrDebug) {
                    list += listOf(
                        "Connected Peers" to it.connectedPeers.joinToString(", ")
                    )
                }

                _data.value = list

            }.launchIn(this)
        }

        bootstrap()
    }

    override fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is LocalEvents.ShowRecoveryPhrase -> {
                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.RecoveryIntro(
                            args = SetupArgs(
                                mnemonic = "",
                                isLightning = true,
                                isShowRecovery = true,
                                greenWallet = greenWallet
                            ),
                        )
                    )
                )
                postSideEffect(SideEffects.Dismiss)
            }

            is LocalEvents.RescanSwaps -> {
                rescanSwaps()
            }

            is LocalEvents.ShareLogs -> {
                shareLogs()
            }

            is LocalEvents.EmptyAccount -> {
                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.RecoverFunds(
                            isSendAll = true,
                        )
                    )
                )
                postSideEffect(SideEffects.Dismiss)
            }
        }
    }

    private fun rescanSwaps() {
        doAsync({
            postSideEffect(SideEffects.Snackbar("id_rescan_swaps_initiated"))
            session.lightningSdkOrNull?.rescanSwaps()
        }, onSuccess = {
            postSideEffect(SideEffects.Snackbar("id_completed"))
        })
    }

    private fun shareLogs() {
        doAsync({
            val file = lightningManager.createLogs()
            postSideEffect(SideEffects.ShareFile(file))
        }, onSuccess = {
            postSideEffect(SideEffects.Snackbar("id_completed"))
        })
    }

    companion object : Loggable()
}

class LightningNodeViewModelPreview : LightningNodeViewModelAbstract(
    greenWallet = previewWallet()
) {

    override val data: StateFlow<List<Pair<String, String>>> = MutableStateFlow(
        listOf(

        )
    )
    override val showEmptyAccount: StateFlow<Boolean> = MutableStateFlow(true)

    companion object {
        fun preview() = LightningNodeViewModelPreview()
    }
}