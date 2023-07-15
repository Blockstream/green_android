package com.blockstream.green.ui.lightning

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import breez_sdk.RecommendedFees
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.green.extensions.boolean
import com.blockstream.green.extensions.string
import com.blockstream.green.ui.wallet.AbstractAssetWalletViewModel
import com.blockstream.green.utils.feeRateWithUnit
import com.blockstream.green.utils.toAmountLook
import com.rickclephas.kmm.viewmodel.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import kotlin.math.absoluteValue

@KoinViewModel
class RecoverFundsViewModel constructor(
    @InjectedParam wallet: GreenWallet,
    @InjectedParam initAccountAsset: AccountAsset,
    @InjectedParam val onChainAddress: String?,
    @InjectedParam val satoshi: Long?
) : AbstractAssetWalletViewModel(
    wallet,
    initAccountAsset
) {
    private val accountAddress = MutableLiveData("") // cached account address
    val address =
        MutableLiveData("") // refund to address, can be set manually of from wallet account
    val amount = MutableLiveData("")

    val recommendedFees = MutableLiveData<RecommendedFees?>(null)

    val feeSlider = MutableLiveData<Float>(1.0f)
    val feeAmountRate = MutableLiveData("") // fee rate

    val hasBitcoinAccount =
        MutableLiveData(session.accounts.value.any { it.isBitcoin && !it.isLightning })
    val showManualAddress = MutableLiveData(!hasBitcoinAccount.boolean())

    val isRefund = MutableLiveData(onChainAddress != null)

    init {
        viewModelScope.coroutineScope.launch {
            recommendedFees.value = withContext(context = Dispatchers.IO) {
                session.lightningSdk.recommendedFees()
            }

            amount.value = satoshi?.absoluteValue.toAmountLook(
                session = session,
                withUnit = true,
            )
        }

        // Cache account address so that switching between manual address, account address is the same
        accountAssetLiveData.asFlow().onEach {
            // If the only available account is LN, show directly the manual address
            if (!it.account.isLightning) {
                accountAddress.value = withContext(context = Dispatchers.IO) {
                    session.getReceiveAddress(it.account).address
                }
            } else {
                showManualAddress.value = true
            }
        }.launchIn(viewModelScope.coroutineScope)

        combine(
            accountAddress.asFlow(),
            showManualAddress.asFlow()
        ) { accountAddress, showManualAddress ->
            accountAddress to showManualAddress
        }.onEach {
            if (!it.second) {
                address.value = it.first
            }
        }.launchIn(viewModelScope.coroutineScope)

        combine(
            feeSlider.asFlow(),
            recommendedFees.asFlow().filterNotNull()
        ) { feeSlider, recommendedFees ->
            feeSlider to recommendedFees
        }.onEach {
            feeAmountRate.value = ((getFee()?.toLong() ?: 0) * 1000).feeRateWithUnit()
        }.launchIn(viewModelScope.coroutineScope)
    }

    private fun getFee(): ULong? {
        return recommendedFees.value?.let {
            when (feeSlider.value?.toInt()) {
                1 -> it.hourFee
                2 -> it.halfHourFee
                3 -> it.fastestFee
                else -> it.economyFee
            }
        }
    }

    fun recoverFunds() {
        doUserAction({
            if (onChainAddress != null) {
                session.lightningSdk.refund(
                    swapAddress = onChainAddress,
                    toAddress = address.string(),
                    satPerVbyte = getFee()?.toUInt()
                )
            } else {
                // Sweep
                session.lightningSdk.sweep(
                    toAddress = address.string(),
                    satPerVbyte = getFee()?.toUInt()
                )
            }

        }, onSuccess = {
            postSideEffect(SideEffects.Success())
        })
    }
}
