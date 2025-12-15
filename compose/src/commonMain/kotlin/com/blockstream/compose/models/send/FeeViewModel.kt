package com.blockstream.compose.models.send

import androidx.lifecycle.viewModelScope
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_network_fee
import com.blockstream.data.data.FeePriority
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.extensions.ifConnected
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.gdk.params.CreateTransactionParams
import com.blockstream.data.lightning.fee
import com.blockstream.data.utils.feeRateWithUnit
import com.blockstream.compose.extensions.launchIn
import com.blockstream.compose.extensions.previewAccountAsset
import com.blockstream.compose.extensions.previewWallet
import com.blockstream.compose.navigation.NavData
import com.blockstream.utils.Loggable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

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
    abstract val feePriorities: StateFlow<List<FeePriority>>
}

class FeeViewModel(
    greenWallet: GreenWallet,
    accountAssetOrNull: AccountAsset?,
    private val useBreezFees: Boolean
) : FeeViewModelAbstract(greenWallet = greenWallet, accountAssetOrNull = accountAssetOrNull) {

    private val _feePriorities: MutableStateFlow<List<FeePriority>> =
        MutableStateFlow(listOf(FeePriority.High(), FeePriority.Medium(), FeePriority.Low()))
    override val feePriorities: StateFlow<List<FeePriority>> = _feePriorities.asStateFlow()

    private var params: CreateTransactionParams? = null

    init {

        viewModelScope.launch {
            _navData.value = NavData(
                title = getString(Res.string.id_network_fee)
            )
        }

        session.ifConnected {
            params = session.pendingTransactionParams

            _network.value = accountAssetOrNull?.account?.network

            _feeEstimation.filterNotNull().onEach {
                params?.also {
                    calculateFees(it)
                }
            }.launchIn(this)

            if (useBreezFees) {
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

    private fun calculateFees(params: CreateTransactionParams) {

        doAsync({
            listOf(FeePriority.High(), FeePriority.Medium(), FeePriority.Low()).map {
                try {
                    val feeRate = getFeeRate(priority = it)

                    val tx = if (params.isRedeposit) session.createRedepositTransaction(
                        network = account.network,
                        params = params.copy(feeRate = feeRate)
                    ) else session.createTransaction(
                        network = account.network,
                        params = params.copy(feeRate = feeRate)
                    )

                    calculateFeePriority(
                        session = session,
                        feePriority = it,
                        feeAmount = tx.fee,
                        feeRate = tx.feeRate?.feeRateWithUnit(),
                        error = tx.error?.takeIf { it.isNotBlank() }
                    )

                } catch (e: Exception) {
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