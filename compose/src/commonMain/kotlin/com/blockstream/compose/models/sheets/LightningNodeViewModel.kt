package com.blockstream.compose.models.sheets

import androidx.lifecycle.viewModelScope
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_account_balance
import blockstream_green.common.generated.resources.id_completed
import blockstream_green.common.generated.resources.id_inbound_liquidity
import blockstream_green.common.generated.resources.id_lightning_disabled_successfully
import blockstream_green.common.generated.resources.id_max_payable_amount
import blockstream_green.common.generated.resources.id_max_receivable_amount
import blockstream_green.common.generated.resources.id_max_single_payment_amount
import blockstream_green.common.generated.resources.id_onchain_balance
import com.blockstream.compose.events.Event
import com.blockstream.compose.extensions.launchIn
import com.blockstream.compose.extensions.previewWallet
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.compose.utils.StringHolder
import com.blockstream.data.data.CredentialType
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.data.SetupArgs
import com.blockstream.data.extensions.lightningMnemonic
import com.blockstream.data.gdk.data.Credentials
import com.blockstream.data.lightning.LightningManager
import com.blockstream.data.lightning.channelsBalanceSatoshi
import com.blockstream.data.lightning.maxPayableSatoshi
import com.blockstream.data.lightning.maxReceivableSatoshi
import com.blockstream.data.lightning.maxSinglePaymentAmountSatoshi
import com.blockstream.data.lightning.onchainBalanceSatoshi
import com.blockstream.data.lightning.totalInboundLiquiditySatoshi
import com.blockstream.data.utils.toAmountLookOrNa
import com.blockstream.utils.Loggable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.inject

abstract class LightningNodeViewModelAbstract(
    greenWallet: GreenWallet
) : GreenViewModel(
    greenWalletOrNull = greenWallet
) {
    abstract val data: StateFlow<List<Pair<StringHolder, StringHolder>>>
    abstract val showEmptyAccount: StateFlow<Boolean>
    abstract val hasLightning: StateFlow<Boolean>
}

class LightningNodeViewModel(greenWallet: GreenWallet) :
    LightningNodeViewModelAbstract(
        greenWallet = greenWallet
    ) {
    override fun screenName(): String = "LightningNodeState"

    private val _data: MutableStateFlow<List<Pair<StringHolder, StringHolder>>> = MutableStateFlow(listOf())
    override val data = _data.asStateFlow()

    override val showEmptyAccount = session.lightningSdk.nodeInfoStateFlow.map {
        session.lightningSdk.nodeInfoStateFlow.value.onchainBalanceSatoshi() > 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    override val hasLightning = session.lightningSdk.nodeInfoStateFlow.map {
        session.hasLightning
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), session.hasLightning)

    val lightningManager: LightningManager by inject()

    class LocalEvents {
        object ShowRecoveryPhrase : Event
        object EmptyAccount : Event
        object ShareDiagnosticData : Event
        object DisableLightning : Event
    }

    init {
        if (session.isConnected) {

            session.lightningSdk.nodeInfoStateFlow.onEach {
                val list = mutableListOf(
                    StringHolder.create("ID") to StringHolder.create(it.id),
                    StringHolder.create(Res.string.id_account_balance) to StringHolder.create(
                        it.channelsBalanceSatoshi().toAmountLookOrNa(
                            session = session,
                            withUnit = true,
                            withGrouping = true
                        )
                    ),
                    StringHolder.create(Res.string.id_onchain_balance) to StringHolder.create(
                        it.onchainBalanceSatoshi().toAmountLookOrNa(
                            session = session,
                            withUnit = true,
                            withGrouping = true
                        )
                    ),
                    StringHolder.create(Res.string.id_inbound_liquidity) to StringHolder.create(
                        it.totalInboundLiquiditySatoshi().toAmountLookOrNa(
                            session = session,
                            withUnit = true,
                            withGrouping = true
                        )
                    ),
                    StringHolder.create(Res.string.id_max_payable_amount) to StringHolder.create(
                        it.maxPayableSatoshi().toAmountLookOrNa(
                            session = session,
                            withUnit = true,
                            withGrouping = true
                        )
                    ),
                    StringHolder.create(Res.string.id_max_single_payment_amount) to StringHolder.create(
                        it.maxSinglePaymentAmountSatoshi()
                            .toAmountLookOrNa(
                                session = session,
                                withUnit = true,
                                withGrouping = true
                            )
                    ),
                    StringHolder.create(Res.string.id_max_receivable_amount) to StringHolder.create(
                        it.maxReceivableSatoshi().toAmountLookOrNa(
                            session = session,
                            withUnit = true,
                            withGrouping = true
                        )
                    )
                )

                if (appInfo.isDevelopmentOrDebug) {
                    list += listOf(
                        StringHolder.create("Connected Peers") to StringHolder.create(it.connectedPeers.joinToString(", "))
                    )
                }

                _data.value = list

            }.launchIn(this)
        }

        bootstrap()
    }


    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is LocalEvents.ShowRecoveryPhrase -> {
                val credentials = if (greenWallet.isHardware) {
                    database.getLoginCredential(
                        id = greenWallet.id,
                        credentialType = CredentialType.KEYSTORE_LIGHTNING_MNEMONIC
                    )?.lightningMnemonic(greenKeystore)?.let { lightningMnemonic ->
                        Credentials(mnemonic = lightningMnemonic)
                    }
                } else {
                    null
                }

                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.RecoveryIntro(
                            setupArgs = SetupArgs(
                                mnemonic = "",
                                isLightningDerived = true,
                                isShowRecovery = true,
                                greenWallet = greenWallet,
                                credentials = credentials
                            ),
                        )
                    )
                )
                postSideEffect(SideEffects.Dismiss)
            }

            is LocalEvents.ShareDiagnosticData -> {
                shareDiagnosticData()
            }

            is LocalEvents.EmptyAccount -> {
                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.RecoverFunds(
                            greenWallet = greenWallet,
                            isEmptyChannels = true,
                        )
                    )
                )
                postSideEffect(SideEffects.Dismiss)
            }

            is LocalEvents.DisableLightning -> {
                onProgress.value = true
                try {
                    if (session.hasLightning) {
                        val account = session.lightningAccount
                        removeAccountUseCase(
                            session = session,
                            wallet = greenWallet,
                            account = account
                        )
                    }

                    postSideEffect(
                        SideEffects.Snackbar(StringHolder.create(Res.string.id_lightning_disabled_successfully))
                    )
                    postSideEffect(SideEffects.Dismiss)

                } catch (error: Exception) {
                    error.printStackTrace()
                    postSideEffect(
                        SideEffects.Snackbar(StringHolder.create(error.message ?: "Error"))
                    )
                } finally {
                    onProgress.value = false
                }
            }
        }
    }

    private fun shareDiagnosticData() {
        doAsync({
            val file = lightningManager.createDiagnosticData(session = session)
            postSideEffect(SideEffects.ShareFile(file))
        }, onSuccess = {
            postSideEffect(SideEffects.Snackbar(StringHolder.create(Res.string.id_completed)))
        })
    }

    companion object : Loggable()
}

class LightningNodeViewModelPreview : LightningNodeViewModelAbstract(
    greenWallet = previewWallet()
) {
    override val data: StateFlow<List<Pair<StringHolder, StringHolder>>> = MutableStateFlow(listOf())
    override val showEmptyAccount: StateFlow<Boolean> = MutableStateFlow(true)
    override val hasLightning: StateFlow<Boolean> = MutableStateFlow(true)

    companion object {
        fun preview() = LightningNodeViewModelPreview()
    }
}