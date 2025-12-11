package com.blockstream.common.models.lightning

import androidx.lifecycle.viewModelScope
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_empty_lightning_account
import blockstream_green.common.generated.resources.id_refund
import blockstream_green.common.generated.resources.id_refund_initiated
import blockstream_green.common.generated.resources.id_sweep
import blockstream_green.common.generated.resources.id_transfer_funds
import blockstream_green.common.generated.resources.id_your_transaction_was
import breez_sdk.RecommendedFees
import breez_sdk.ReverseSwapFeesRequest
import com.blockstream.common.data.Denomination
import com.blockstream.common.data.FeePriority
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.SupportData
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.ifConnected
import com.blockstream.common.extensions.ifConnectedSuspend
import com.blockstream.common.extensions.isBlank
import com.blockstream.common.extensions.previewAccountAsset
import com.blockstream.common.extensions.previewAccountAssetBalance
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.AccountAssetBalance
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.StringHolder
import com.blockstream.common.utils.feeRateWithUnit
import com.blockstream.common.utils.getStringFromIdOrNull
import com.blockstream.common.utils.toAmountLook
import com.blockstream.green.utils.Loggable
import com.blockstream.ui.events.Event
import com.blockstream.ui.navigation.NavData
import com.blockstream.ui.sideeffects.SideEffect
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.jetbrains.compose.resources.getString
import kotlin.math.absoluteValue

abstract class RecoverFundsViewModelAbstract(
    greenWallet: GreenWallet,
    val isSendAll: Boolean,
    val onChainAddress: String?
) : GreenViewModel(greenWalletOrNull = greenWallet) {

    val isRefund by lazy {
        onChainAddress != null
    }

    override fun screenName(): String =
        if (isRefund) "OnChainRefund" else if (isSendAll) "LightningSendAll" else "RedeemOnchainFunds"
    abstract val bitcoinAccounts: StateFlow<List<AccountAssetBalance>>
    abstract val manualAddress: MutableStateFlow<String>
    abstract val amount: StateFlow<String>
    abstract val amountToBeRefunded: StateFlow<String?>
    abstract val amountToBeRefundedFiat: StateFlow<String?>
    abstract val hasBitcoinAccount: StateFlow<Boolean>
    abstract val showManualAddress: MutableStateFlow<Boolean>
    abstract val recommendedFees: StateFlow<RecommendedFees?>
    abstract val onProgressSending: StateFlow<Boolean>
    abstract val feePriority: StateFlow<FeePriority>

    abstract val error: StateFlow<String?>
}

class RecoverFundsViewModel(
    greenWallet: GreenWallet,
    isSendAll: Boolean,
    onChainAddress: String?,
    val satoshi: Long,
) : RecoverFundsViewModelAbstract(
    greenWallet = greenWallet,
    isSendAll = isSendAll,
    onChainAddress = onChainAddress
) {
    override val manualAddress: MutableStateFlow<String> = MutableStateFlow("")

    override val amountToBeRefunded: MutableStateFlow<String?> =
        MutableStateFlow(null)
    override val amountToBeRefundedFiat: MutableStateFlow<String?> =
        MutableStateFlow(null)

    private val _onProgressSending: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val onProgressSending: StateFlow<Boolean> = _onProgressSending.asStateFlow()

    private var _customFeeRate: MutableStateFlow<Long?> = MutableStateFlow(null)
    private val _feePriority: MutableStateFlow<FeePriority> = MutableStateFlow(FeePriority.Low())
    override val feePriority: StateFlow<FeePriority> = _feePriority.asStateFlow()

    // Used to trigger
    private val _feePriorityPrimitive: StateFlow<FeePriority> = _feePriority.map { it.primitive() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, FeePriority.Low())

    private val _error: MutableStateFlow<String?> = MutableStateFlow(null)
    override val error: StateFlow<String?> = _error.asStateFlow()

    override val amount: StateFlow<String> = flow {
        session.ifConnectedSuspend {
            ((if (isSendAll) session.accountAssets(session.lightningAccount).value.policyAsset else satoshi.absoluteValue).toAmountLook(
                session = session,
                withUnit = true,
            ) ?: "").also {
                emit(it)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")

    override val recommendedFees: StateFlow<RecommendedFees?> = flow {
        session.ifConnectedSuspend {
            emit(
                session.lightningSdk.recommendedFees().also {
                    _customFeeRate.value = it?.minimumFee?.toLong()
                }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    override val bitcoinAccounts = session.accounts.map { accounts ->
        accounts.filter { it.isBitcoin && !it.isLightning }.map {
            AccountAssetBalance.create(accountAsset = it.accountAsset, session = sessionOrNull)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, listOf())

    override val hasBitcoinAccount: StateFlow<Boolean> = bitcoinAccounts.map {
        it.isNotEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)

    override val showManualAddress: MutableStateFlow<Boolean> =
        MutableStateFlow(false)

    class LocalEvents {
        data class ClickFeePriority(val showCustomFeeRateDialog: Boolean = false) : Event
        data class SetFeeRate(val feePriority: FeePriority) : Event
        data class SetCustomFeeRate(val amount: String) : Event
    }

    class LocalSideEffects {
        data class ShowCustomFeeRate(val feeRate: Long) : SideEffect
    }

    init {
        viewModelScope.launch {
            _navData.value = NavData(
                title = getString(
                    when {
                        isRefund -> Res.string.id_refund
                        isSendAll -> Res.string.id_empty_lightning_account
                        else -> Res.string.id_sweep
                    }
                ),
                subtitle = greenWallet.name
            )
        }

        sessionOrNull?.ifConnected {
            accountAsset.value =
                session.accounts.value.firstOrNull { it.isBitcoin && !it.isLightning }?.accountAsset
            showManualAddress.value = accountAsset.value == null

            var prepareJob: Job? = null
            combine(
                manualAddress,
                accountAsset,
                showManualAddress,
                _feePriorityPrimitive,
                recommendedFees.filterNotNull()
            ) { _ ->
                prepareJob?.cancel()
                prepareJob = prepare()
            }.launchIn(viewModelScope)
        }

        bootstrap()
    }

    private fun showCustomFeeRateDialog() {
        postSideEffect(
            LocalSideEffects.ShowCustomFeeRate(
                (_customFeeRate.value ?: 0L).coerceAtLeast(minFee())
            )
        )
    }

    override suspend fun handleEvent(event: Event) {
        if (event is LocalEvents.SetFeeRate) {
            if (event.feePriority is FeePriority.Custom && event.feePriority.customFeeRate.isNaN()) {

                // Prevent replacing if rate is the same as it will clear the fee estimation data
                val customFeeRate = _customFeeRate.value ?: minFee()
                FeePriority.Custom(
                    customFeeRate = customFeeRate.toDouble(),
                    feeRate = feeRate(customFeeRate)
                ).also {
                    if (_feePriorityPrimitive.value != it.primitive()) {
                        _feePriority.value = it
                    }
                }
                showCustomFeeRateDialog()
            } else {
                _feePriority.value = event.feePriority
            }
        } else if (event is LocalEvents.ClickFeePriority) {
            if (event.showCustomFeeRateDialog && feePriority.value is FeePriority.Custom) {
                showCustomFeeRateDialog()
            } else {
                postSideEffect(
                    SideEffects.OpenFeeBottomSheet(
                        greenWallet = greenWallet,
                        accountAsset = null,
                        params = null,
                        useBreezFees = true
                    )
                )
            }
        } else if (event is LocalEvents.SetCustomFeeRate) {
            setCustomFeeRate(event.amount)
        } else if (event is Events.Continue) {
            doAction()
        } else {
            super.handleEvent(event)
        }
    }

    private fun updateFee(
        fee: String? = null,
        feeFiat: String? = null,
        feeRate: String? = null,
        error: String? = null
    ) {
        _feePriority.value = _feePriority.value.let {
            when (it) {
                is FeePriority.Custom -> FeePriority.Custom(
                    customFeeRate = it.customFeeRate,
                    fee = fee ?: it.fee,
                    feeFiat = feeFiat ?: it.feeFiat,
                    feeRate = feeRate ?: it.feeRate,
                    error = error
                )

                is FeePriority.High -> FeePriority.High(
                    fee = fee ?: it.fee,
                    feeFiat = feeFiat ?: it.feeFiat,
                    feeRate = feeRate ?: it.feeRate,
                    error = error
                )

                is FeePriority.Low -> FeePriority.Low(
                    fee = fee ?: it.fee,
                    feeFiat = feeFiat ?: it.feeFiat,
                    feeRate = feeRate ?: it.feeRate,
                    error = error
                )

                is FeePriority.Medium -> FeePriority.Medium(
                    fee = fee ?: it.fee,
                    feeFiat = feeFiat ?: it.feeFiat,
                    feeRate = feeRate ?: it.feeRate,
                    error = error
                )
            }
        }
    }

    private suspend fun address() =
        if (showManualAddress.value) manualAddress.value else session.getReceiveAddress(account).address

    private fun minFee(): Long = recommendedFees.value?.minimumFee?.toLong() ?: 0

    private fun setCustomFeeRate(amount: String? = null) {
        val minFee = minFee()

        if (amount == null) {
            _customFeeRate.value = minFee
        } else {
            (amount.toLongOrNull() ?: 0).also {
                if (it < minFee) {
                    postSideEffect(SideEffects.ErrorSnackbar(Exception("id_fee_rate_must_be_at_least_s|${minFee}")))
                } else {
                    _customFeeRate.value = it
                }
            }
        }

        // Prevent replacing if rate is the same as it will clear the fee estimation data
        val customFeeRate = _customFeeRate.value ?: minFee
        FeePriority.Custom(
            customFeeRate = customFeeRate.toDouble(),
            feeRate = (customFeeRate * 1000).feeRateWithUnit()
        ).also {
            if (_feePriorityPrimitive.value != it.primitive()) {
                _feePriority.value = it
            }
        }
    }

    private fun feeRate(fee: Long) = (fee * 1000).feeRateWithUnit()

    private val mutex = Mutex()
    private fun prepare(): Job {
        val feeRate = feeRate(getFee()?.toLong() ?: 0L)

        return doAsync(mutex = mutex, action = {
            val address = address()
            if (address.isBlank()) {
                return@doAsync false
            }

            if (isSendAll) {
                val maxReverseSwapAmount = session.lightningSdk.onchainPaymentLimits().maxPayableSat
                val minAmount =
                    session.lightningSdk.fetchReverseSwapFees(ReverseSwapFeesRequest()).min

                if (maxReverseSwapAmount < minAmount) {
                    throw Exception(
                        "id_you_can_empty_your_lightning|${
                            minAmount.toLong().toAmountLook(
                                session = session,
                                denomination = Denomination.SATOSHI,
                                withGrouping = true,
                                withUnit = false,
                            )
                        }"
                    )
                }

                val reverseSwapInfo = session.lightningSdk.fetchReverseSwapFees(
                    ReverseSwapFeesRequest(maxReverseSwapAmount)
                )
                val totalFees = reverseSwapInfo.totalFees

                amountToBeRefunded.value = (maxReverseSwapAmount - (totalFees ?: 0u)).toLong()
                    .toAmountLook(session = session, withUnit = true)
                amountToBeRefundedFiat.value = (maxReverseSwapAmount - (totalFees ?: 0u)).toLong()
                    .toAmountLook(
                        session = session,
                        withUnit = true,
                        denomination = Denomination.fiat(session)
                    )

                totalFees?.toLong()?.let {
                    updateFee(
                        fee = it.toAmountLook(session = session, withUnit = true) ?: "",
                        feeFiat = it.toAmountLook(
                            session = session,
                            withUnit = true,
                            denomination = Denomination.fiat(session)
                        ) ?: "",
                        feeRate = feeRate
                    )
                } ?: run {
                    updateFee("", "")
                }

                return@doAsync true
            } else if (isRefund) {
                // Refund from OnChain address
                session.lightningSdk.prepareRefund(
                    swapAddress = onChainAddress ?: "",
                    toAddress = address,
                    satPerVbyte = getFee()?.toUInt()
                ).also {
                    if (satoshi - it.refundTxFeeSat.toLong() < 0) {
                        throw Exception("id_insufficient_funds")
                    }

                    amountToBeRefunded.value = (satoshi - it.refundTxFeeSat.toLong()).toAmountLook(
                        session = session,
                        withUnit = true
                    )
                    amountToBeRefundedFiat.value =
                        (satoshi - it.refundTxFeeSat.toLong()).toAmountLook(
                            session = session,
                            withUnit = true,
                            denomination = Denomination.fiat(session)
                        )

                    it.refundTxFeeSat.toLong().let {
                        updateFee(
                            it.toAmountLook(session = session, withUnit = true) ?: "",
                            it.toAmountLook(
                                session = session,
                                withUnit = true,
                                denomination = Denomination.fiat(session)
                            ) ?: "",
                            feeRate = feeRate
                        )
                    }
                }

                return@doAsync true
            } else {
                // Redeem Onchain funds from Lightning node
                session.lightningSdk.prepareRedeemOnchainFunds(
                    toAddress = address,
                    satPerVbyte = getFee()?.toUInt()
                ).also {
                    val toBeRefunded = satoshi - it.txFeeSat.toLong()
                    if (toBeRefunded < 0) {
                        throw Exception("id_insufficient_funds")
                    }

                    amountToBeRefunded.value = toBeRefunded.toAmountLook(
                        session = session,
                        withUnit = true
                    )
                    amountToBeRefundedFiat.value = toBeRefunded.toAmountLook(
                        session = session,
                        withUnit = true,
                        denomination = Denomination.fiat(session)
                    )

                    it.txFeeSat.toLong().let {
                        updateFee(
                            fee = it.toAmountLook(session = session, withUnit = true) ?: "",
                            feeFiat = it.toAmountLook(
                                session = session,
                                withUnit = true,
                                denomination = Denomination.fiat(session)
                            ) ?: "",
                            feeRate = feeRate
                        )
                    }
                }

                return@doAsync true
            }
        }, onError = {
            if (it.message?.contains("Insufficient funds to pay fees") == true) {
                updateFee(error = "id_insufficient_funds", feeRate = feeRate)
                _error.value = null
            } else {
                _error.value = getStringFromIdOrNull(it.message)
            }
            _isValid.value = false
            amountToBeRefunded.value = null
            amountToBeRefundedFiat.value = null
        }, onSuccess = {
            _error.value = null
            _isValid.value = it
        })
    }

    private fun getFee(): ULong? {
        return recommendedFees.value?.let { fees ->
            feePriority.value.let {
                when (it) {
                    is FeePriority.High -> fees.fastestFee
                    is FeePriority.Medium -> fees.hourFee
                    is FeePriority.Low -> fees.economyFee
                    is FeePriority.Custom -> it.customFeeRate.toULong()
                }
            }
        }
    }

    private fun doAction() {
        doAsync({
            val address =
                if (showManualAddress.value) manualAddress.value else session.getReceiveAddress(
                    account
                ).address

            if (isSendAll) {
                // Send Onchain all funds emptying the wallet
                session.lightningSdk.payOnchain(
                    address = address,
                    satPerVbyte = getFee()?.toUInt()
                )
                session.emptyLightningAccount()
            } else if (isRefund) {
                // Refund from OnChain address
                session.lightningSdk.refund(
                    swapAddress = onChainAddress ?: "",
                    toAddress = address,
                    satPerVbyte = getFee()?.toUInt()
                ).refundTxId
            } else {
                // Redeem Onchain funds from Lightning node
                session.lightningSdk.redeemOnchainFunds(
                    toAddress = address,
                    satPerVbyte = getFee()?.toUInt()
                ).txid
            }
        }, preAction = {
            onProgress.value = true
            _onProgressSending.value = true
        }, postAction = {
            onProgress.value = false
            _onProgressSending.value = it == null
        }, onSuccess = {
            if (isRefund) {
                postSideEffect(
                    SideEffects.NavigateBack(
                        title = StringHolder.create(Res.string.id_refund),
                        message = StringHolder.create(Res.string.id_refund_initiated)
                    )
                )
            } else {
                postSideEffect(
                    SideEffects.NavigateBack(
                        title = StringHolder.create(Res.string.id_transfer_funds),
                        message = StringHolder.create(Res.string.id_your_transaction_was)
                    )
                )
            }
        })
    }

    override fun errorReport(exception: Throwable): SupportData {
        return SupportData.create(
            throwable = exception,
            network = session.lightning,
            session = session
        )
    }

    companion object : Loggable()
}

class RecoverFundsViewModelPreview(greenWallet: GreenWallet) : RecoverFundsViewModelAbstract(
    greenWallet = greenWallet,
    isSendAll = false,
    onChainAddress = null
) {

    override val bitcoinAccounts: StateFlow<List<AccountAssetBalance>> = MutableStateFlow(
        listOf(
            previewAccountAssetBalance()
        )
    )

    override val manualAddress: MutableStateFlow<String> = MutableStateFlow("")
    override val amount: StateFlow<String> = MutableStateFlow("1 BTC")
    override val hasBitcoinAccount: StateFlow<Boolean> = MutableStateFlow(false)
    override val showManualAddress: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val recommendedFees: StateFlow<RecommendedFees?> = MutableStateFlow(null)
    override val onProgressSending: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val feePriority: StateFlow<FeePriority> = MutableStateFlow(FeePriority.Low())
    override val error: StateFlow<String?> = MutableStateFlow(null)
    override val amountToBeRefunded: MutableStateFlow<String?> = MutableStateFlow("1 BTC")
    override val amountToBeRefundedFiat: MutableStateFlow<String?> = MutableStateFlow("10000 USD")

    init {
        accountAsset.value = previewAccountAsset()

        onProgress.value = true

        viewModelScope.launch {
            delay(3000)
            onProgressSending.value = true
            delay(3000)
            onProgress.value = false
            onProgressSending.value = false
        }
    }

    companion object {
        fun preview(): RecoverFundsViewModelPreview {
            return RecoverFundsViewModelPreview(
                greenWallet = previewWallet(isHardware = false)
            )
        }
    }
}