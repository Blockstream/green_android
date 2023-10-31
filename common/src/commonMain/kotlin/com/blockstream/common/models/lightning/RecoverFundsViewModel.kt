package com.blockstream.common.models.lightning

import breez_sdk.RecommendedFees
import com.blockstream.common.data.ErrorReport
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.Loggable
import com.blockstream.common.utils.feeRateWithUnit
import com.blockstream.common.utils.toAmountLook
import com.rickclephas.kmm.viewmodel.coroutineScope
import com.rickclephas.kmm.viewmodel.stateIn
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue


abstract class RecoverFundsViewModelAbstract(greenWallet: GreenWallet, val onChainAddress: String?) :
    GreenViewModel(greenWalletOrNull = greenWallet) {

    val isRefund by lazy {
        onChainAddress != null
    }

    val isSweep by lazy {
        onChainAddress == null
    }

    override fun screenName(): String = if(isRefund) "OnChainRefund" else "LightningSweep"

    @NativeCoroutinesState
    abstract val address: MutableStateFlow<String>

    @NativeCoroutinesState
    abstract val amount: StateFlow<String>

    @NativeCoroutinesState
    abstract val feeSlider: MutableStateFlow<Int>

    @NativeCoroutinesState
    abstract val feeAmountRate: StateFlow<String>

    @NativeCoroutinesState
    abstract val hasBitcoinAccount: StateFlow<Boolean>

    @NativeCoroutinesState
    abstract val showManualAddress: MutableStateFlow<Boolean>

    @NativeCoroutinesState
    abstract val recommendedFees: StateFlow<RecommendedFees?>
}

class RecoverFundsViewModel(
    greenWallet: GreenWallet,
    onChainAddress: String?,
    val satoshi: Long,
) : RecoverFundsViewModelAbstract(greenWallet = greenWallet, onChainAddress = onChainAddress) {
    private val accountAddress = MutableStateFlow("") // cached account address

    override val address: MutableStateFlow<String> = MutableStateFlow("")

    override val amount: StateFlow<String> = flow {
        emit(
            satoshi.absoluteValue.toAmountLook(
                session = session,
                withUnit = true,
            ) ?: ""
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")

    override val recommendedFees: StateFlow<RecommendedFees?> = flow {
        emit(session.lightningSdk.recommendedFees())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    override val feeSlider: MutableStateFlow<Int> = MutableStateFlow(1)

    private val _feeAmountRate: MutableStateFlow<String> = MutableStateFlow("")
    override val feeAmountRate: StateFlow<String> = _feeAmountRate.asStateFlow()

    private val bitcoinAccounts = session.accounts.map { accounts ->
        accounts.filter { it.isBitcoin && !it.isLightning }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), session.accounts.value.filter { it.isBitcoin && !it.isLightning })

    override val hasBitcoinAccount: StateFlow<Boolean> = session.accounts.map { accounts ->
        accounts.any { it.isBitcoin && !it.isLightning }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), bitcoinAccounts.value.isNotEmpty())

    override val showManualAddress: MutableStateFlow<Boolean> = MutableStateFlow(hasBitcoinAccount.value)

    override fun handleEvent(event: Event) {
        super.handleEvent(event)

        if (event is Events.Continue) {
            recoverFunds()
        }
    }

    init {
        bootstrap()

        accountAsset.value = bitcoinAccounts.value.firstOrNull()?.let { AccountAsset.fromAccount(it) }
        showManualAddress.value = accountAsset.value == null

        // Cache account address so that switching between manual address, account address is the same
        accountAsset.filterNotNull().onEach {
            accountAddress.value = withContext(context = Dispatchers.IO) {
                session.getReceiveAddress(it.account).address
            }
        }.launchIn(viewModelScope.coroutineScope)

        combine(
            accountAddress,
            showManualAddress
        ) { accountAddress, showManualAddress ->
            accountAddress to showManualAddress
        }.onEach {
            if (!it.second) {
                address.value = it.first
            }
        }.launchIn(viewModelScope.coroutineScope)

        combine(
            feeSlider,
            recommendedFees.filterNotNull()
        ) { feeSlider, recommendedFees ->
            feeSlider to recommendedFees
        }.onEach {
            _feeAmountRate.value = ((getFee()?.toLong() ?: 0) * 1000).feeRateWithUnit()
        }.launchIn(viewModelScope.coroutineScope)

    }

    private fun getFee(): ULong? {
        return recommendedFees.value?.let {
            when (feeSlider.value.toInt()) {
                1 -> it.hourFee
                2 -> it.halfHourFee
                3 -> it.fastestFee
                else -> it.economyFee
            }
        }
    }

    private fun recoverFunds() {
        doAsync({
            if (onChainAddress != null) {
                // Refund from OnChain address
                session.lightningSdk.refund(
                    swapAddress = onChainAddress,
                    toAddress = address.value,
                    satPerVbyte = getFee()?.toUInt()
                ).refundTxId
            } else {
                // Sweep from Lightning node
                session.lightningSdk.sweep(
                    toAddress = address.value,
                    satPerVbyte = getFee()?.toUInt()
                ).txid
            }
        }, onSuccess = {
            postSideEffect(SideEffects.Success())
        })
    }

    override fun errorReport(exception: Throwable): ErrorReport {
        return ErrorReport.create(throwable = exception, network = session.lightning, session = session)
    }

    companion object : Loggable()
}

class RecoverFundsViewModelPreview(greenWallet: GreenWallet) :
    RecoverFundsViewModelAbstract(greenWallet = greenWallet, onChainAddress = null) {
    companion object {
        fun preview(): RecoverFundsViewModelPreview {
            return RecoverFundsViewModelPreview(
                greenWallet = previewWallet(isHardware = false)
            )
        }
    }

    override val address: MutableStateFlow<String> = MutableStateFlow("")
    override val amount: StateFlow<String> = MutableStateFlow("")
    override val feeSlider: MutableStateFlow<Int> = MutableStateFlow(1)
    override val feeAmountRate: StateFlow<String> = MutableStateFlow("")
    override val hasBitcoinAccount: StateFlow<Boolean> = MutableStateFlow(false)
    override val showManualAddress: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val recommendedFees: StateFlow<RecommendedFees?> = MutableStateFlow(null)
}