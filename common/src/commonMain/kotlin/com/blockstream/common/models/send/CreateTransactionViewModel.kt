package com.blockstream.common.models.send

import com.blockstream.common.AddressInputType
import com.blockstream.common.data.Denomination
import com.blockstream.common.data.FeePriority
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.events.Event
import com.blockstream.common.extensions.ifConnected
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.gdk.FeeBlockHigh
import com.blockstream.common.gdk.FeeBlockLow
import com.blockstream.common.gdk.FeeBlockMedium
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.params.CreateTransactionParams
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.Loggable
import com.blockstream.common.utils.ifNotNull
import com.blockstream.common.utils.toAmountLook
import com.rickclephas.kmm.viewmodel.stateIn
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach


abstract class CreateTransactionViewModelAbstract(
    greenWallet: GreenWallet,
    accountAssetOrNull: AccountAsset? = null,
) :
    GreenViewModel(greenWalletOrNull = greenWallet, accountAssetOrNull = accountAssetOrNull) {

    override fun segmentation(): HashMap<String, Any>? {
        return countly.sessionSegmentation(session = session)
    }

    internal val _network: MutableStateFlow<Network?> =
        MutableStateFlow(accountAssetOrNull?.account?.network)

    internal val _feePriority: MutableStateFlow<FeePriority> = MutableStateFlow(FeePriority.Low())
    internal val _feeEstimation: MutableStateFlow<List<Long>?> = MutableStateFlow(null)

    // Used to trigger
    internal val _feePriorityPrimitive: StateFlow<FeePriority> = _feePriority.map { it.primitive() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, FeePriority.Low())

    internal var _addressInputType: AddressInputType? = null

    @NativeCoroutinesState
    val feePriority: StateFlow<FeePriority> = _feePriority.asStateFlow()

    private var _customFeeRate: MutableStateFlow<Double?> = MutableStateFlow(null)

    @NativeCoroutinesState
    val customFeeRate: StateFlow<Double?> = _customFeeRate.asStateFlow()

    internal var _transactionParams: CreateTransactionParams? = null

    class LocalEvents {
        object ClickFeePriority : Event
        data class SetFeeRate(val feePriority: FeePriority) : Event
        data class SetCustomFeeRate(val amount: String) : Event
    }

    class LocalSideEffects {
        data class ShowCustomFeeRate(val feeRate: Double) : SideEffect
    }

    override fun bootstrap() {
        super.bootstrap()

        session.ifConnected {
            _network.filterNotNull().onEach {
                _feeEstimation.value = session.getFeeEstimates(it).fees
                _customFeeRate.value = _customFeeRate.value ?: ((_feeEstimation.value?.firstOrNull()
                    ?: it.defaultFee) / 1000.0)
            }.launchIn(this)
        }
    }

    override fun handleEvent(event: Event) {
        super.handleEvent(event)

        if (event is LocalEvents.SetFeeRate) {
            if (event.feePriority is FeePriority.Custom && event.feePriority.customFeeRate.isNaN()) {
                _feePriority.value = FeePriority.Custom(_customFeeRate.value ?: minFee())
                showCustomFeeRateDialog()
            } else {
                _feePriority.value = event.feePriority
            }
        } else if (event is LocalEvents.ClickFeePriority) {
            ifNotNull(accountAsset.value, _transactionParams) { acc, params ->
                postSideEffect(
                    SideEffects.OpenFeeBottomSheet(
                        greenWallet = greenWallet,
                        accountAsset = acc,
                        params = params
                    )
                )
            }
        } else if (event is LocalEvents.SetCustomFeeRate) {
            setCustomFeeRate(event.amount)
        }
    }

    private fun minFee(): Double =
        (_feeEstimation.value?.firstOrNull() ?: _network.value?.defaultFee
        ?: session.defaultNetwork.defaultFee) / 1000.0

    private fun setCustomFeeRate(amount: String? = null) {
        val minFee = minFee()

        if (amount == null) {
            _customFeeRate.value = minFee
        } else {
            (amount.toDoubleOrNull() ?: 0.0).also {
                if (it < minFee) {
                    postSideEffect(SideEffects.ErrorSnackbar(Exception("id_fee_rate_must_be_at_least_s|$minFee")))
                } else {
                    _customFeeRate.value = it
                }
            }
        }

        _feePriority.value = FeePriority.Custom(_customFeeRate.value ?: minFee)
    }

    internal suspend fun calculateFeePriority(
        session: GdkSession,
        feePriority: FeePriority,
        feeAmount: Long? = null,
        feeRate: String? = null,
        error: String? = null
    ): FeePriority {
        logger.d { "calculateFeePriority" }
        val fee = feeAmount.toAmountLook(session = session, withUnit = true)
        val feeFiat = feeAmount.toAmountLook(
            session = session,
            withUnit = true,
            denomination = Denomination.fiat(session)
        )
        val expectedConfirmationTime = _network.value?.let { network ->
            val blocksPerHour = network.blocksPerHour

            getFeeRate(feePriority).let { feeRate ->
                _feeEstimation.value?.withIndex()?.indexOfFirst {
                    // Skip relay value
                    if (it.index == 0) {
                        false
                    } else {
                        feeRate >= it.value
                    }
                }.takeIf { it != -1 } ?: 10
            }.let { blocks ->
                val n =
                    if (blocks % blocksPerHour == 0) blocks / blocksPerHour else blocks * (60 / blocksPerHour)
                when {
                    blocks % blocksPerHour != 0 -> "mins"
                    blocks == blocksPerHour -> "h"
                    else -> "h"
                }.let {
                    "~ $n $it"
                }
            }
        }

        return when (feePriority) {
            is FeePriority.Custom -> feePriority.copy(
                fee = fee,
                feeFiat = feeFiat,
                feeRate = feeRate,
                error = error,
                expectedConfirmationTime = expectedConfirmationTime
            )

            is FeePriority.High -> feePriority.copy(
                fee = fee,
                feeFiat = feeFiat,
                feeRate = feeRate,
                error = error,
                expectedConfirmationTime = expectedConfirmationTime
            )

            is FeePriority.Low -> feePriority.copy(
                fee = fee,
                feeFiat = feeFiat,
                feeRate = feeRate,
                error = error,
                expectedConfirmationTime = expectedConfirmationTime
            )

            is FeePriority.Medium -> feePriority.copy(
                fee = fee,
                feeFiat = feeFiat,
                feeRate = feeRate,
                error = error,
                expectedConfirmationTime = expectedConfirmationTime
            )
        }
    }

    private fun showCustomFeeRateDialog() {
        postSideEffect(LocalSideEffects.ShowCustomFeeRate(_customFeeRate.value ?: 0.0))
    }

    internal fun getFeeRate(priority: FeePriority? = null): Long =
        (priority ?: feePriority.value).let {
            if (it is FeePriority.Custom) {
                (it.customFeeRate.coerceAtLeast(minFee()) * 1000).toLong()
            } else {
                when (it) {
                    is FeePriority.High -> FeeBlockHigh
                    is FeePriority.Medium -> FeeBlockMedium
                    else -> FeeBlockLow
                }.let { index ->
                    _feeEstimation.value?.getOrNull(index)
                } ?: _network.value?.defaultFee?.coerceAtLeast(
                    _feeEstimation.value?.getOrNull(0) ?: 0
                )
                ?: 0
            }
        }

    companion object : Loggable()
}