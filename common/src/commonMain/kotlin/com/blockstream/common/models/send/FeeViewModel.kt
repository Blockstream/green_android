package com.blockstream.common.models.send

import com.blockstream.common.data.FeePriority
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.NavData
import com.blockstream.common.extensions.ifConnected
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.extensions.previewAccountAsset
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.params.CreateTransactionParams
import com.blockstream.common.lightning.fee
import com.blockstream.common.utils.Loggable
import com.blockstream.common.utils.feeRateWithUnit
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach

abstract class FeeViewModelAbstract(
    greenWallet: GreenWallet,
    accountAssetOrNull: AccountAsset?
) : CreateTransactionViewModelAbstract(
    greenWallet = greenWallet,
    accountAssetOrNull = accountAssetOrNull
) {
    override fun screenName(): String = "Fee"

    override fun segmentation(): HashMap<String, Any>? {
        return countly.accountSegmentation(session = session, account = accountOrNull)
    }

    @NativeCoroutinesState
    abstract val feePriorities: StateFlow<List<FeePriority>>
}

class FeeViewModel(
    greenWallet: GreenWallet,
    accountAssetOrNull: AccountAsset?,
    val params: CreateTransactionParams?,
    val useBreezFees: Boolean
) : FeeViewModelAbstract(greenWallet = greenWallet, accountAssetOrNull = accountAssetOrNull) {

    private val _feePriorities: MutableStateFlow<List<FeePriority>> =
        MutableStateFlow(listOf(FeePriority.High(), FeePriority.Medium(), FeePriority.Low()))
    override val feePriorities: StateFlow<List<FeePriority>> = _feePriorities.asStateFlow()


    init {
        _navData.value = NavData(
            title = "id_network_fee"
        )

        session.ifConnected {
            _network.value = accountAssetOrNull?.account?.network

            _feeEstimation.filterNotNull().onEach {
                calculateFees()
            }.launchIn(this)

            if(useBreezFees){
                calculateBreezFees()
            }
        }

        bootstrap()
    }

    private fun calculateBreezFees() {
        doAsync({
            sessionOrNull?.lightningSdkOrNull?.recommendedFees()?.let { recommendedFees ->
                listOf(FeePriority.High(), FeePriority.Medium(), FeePriority.Low()).map {

                    val feeRate = (recommendedFees.fee(it) * 1000).feeRateWithUnit()
                    when (it) {
                        is FeePriority.Custom -> it.copy(
                            feeRate = feeRate,
                        )

                        is FeePriority.High -> it.copy(
                            feeRate = feeRate,
                        )

                        is FeePriority.Low -> it.copy(
                            feeRate = feeRate,
                        )

                        is FeePriority.Medium -> it.copy(
                            feeRate = feeRate,
                        )
                    }
                }
            }
        }, onSuccess = { fees ->
            fees?.also {
                _feePriorities.value = it
            }
        })
    }

    private fun calculateFees() {
        if(params == null) return

        doAsync({
            listOf(FeePriority.High(), FeePriority.Medium(), FeePriority.Low()).map {
                try{
                    val feeRate = getFeeRate(priority = it)
                    val tx = session.createTransaction(
                        account.network,
                        params.copy(
                            addressees = listOfNotNull(params.addresseesAsParams?.firstOrNull()?.toJsonElement()),
                            feeRate = feeRate
                        )
                    )

                    calculateFeePriority(
                        session = session,
                        feePriority = it,
                        feeAmount = tx.fee,
                        feeRate = tx.feeRate?.feeRateWithUnit(),
                        error = tx.error?.takeIf { it.isNotBlank() }
                    )

                }catch (e: Exception){
                    e.message

                    calculateFeePriority(
                        session = session,
                        feePriority = it,
                        error = e.message
                    )
                }
            }
        }, onSuccess = {
            _feePriorities.value = it
        })
    }

    companion object : Loggable()
}

class FeeViewModelPreview(greenWallet: GreenWallet) :
    FeeViewModelAbstract(greenWallet = greenWallet, accountAssetOrNull = previewAccountAsset()) {

    override val feePriorities: StateFlow<List<FeePriority>> =
        MutableStateFlow(listOf(FeePriority.High(error = "id_insufficient_funds"), FeePriority.Medium(), FeePriority.Low()))


    companion object {
        fun preview() = FeeViewModelPreview(previewWallet())
    }
}