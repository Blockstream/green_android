package com.blockstream.green.ui.lightning

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import breez_sdk.RecommendedFees
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.green.data.Countly
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.extensions.boolean
import com.blockstream.green.extensions.string
import com.blockstream.green.managers.SessionManager
import com.blockstream.green.ui.wallet.AbstractAssetWalletViewModel
import com.blockstream.green.utils.ConsumableEvent
import com.blockstream.green.utils.feeRateWithUnit
import com.blockstream.green.utils.toAmountLook
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue


class RecoverFundsViewModel @AssistedInject constructor(
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    countly: Countly,
    @Assisted wallet: Wallet,
    @Assisted initAccountAsset: AccountAsset,
    @Assisted val onChainAddress: String?,
    @Assisted val satoshi: Long?
) : AbstractAssetWalletViewModel(
    sessionManager,
    walletRepository,
    countly,
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
        MutableLiveData(session.accounts.any { it.isBitcoin && !it.isLightning })
    val showManualAddress = MutableLiveData(!hasBitcoinAccount.boolean())

    val isRefund = MutableLiveData(onChainAddress != null)

    init {
        lifecycleScope.launch {
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
            accountAddress.value = withContext(context = Dispatchers.IO) {
                session.getReceiveAddress(it.account).address
            }
        }.launchIn(viewModelScope)

        combine(
            accountAddress.asFlow(),
            showManualAddress.asFlow()
        ) { accountAddress, showManualAddress ->
            accountAddress to showManualAddress
        }.onEach {
            if (!it.second) {
                address.value = it.first
            }
        }.launchIn(viewModelScope)

        combine(
            feeSlider.asFlow(),
            recommendedFees.asFlow().filterNotNull()
        ) { feeSlider, recommendedFees ->
            feeSlider to recommendedFees
        }.onEach {
            feeAmountRate.value = ((getFee()?.toLong() ?: 0) * 1000).feeRateWithUnit()
        }.launchIn(viewModelScope)
    }

    private fun getFee(): UInt? {
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
                    satPerVbyte = getFee()
                )
            } else {
                // Close channel
                session.lightningSdk.closeLspChannels()
                // Sweep
                session.lightningSdk.sweep(
                    toAddress = address.string(),
                    satPerVbyte = getFee()
                )
            }

        }, onSuccess = {
            onEvent.postValue(ConsumableEvent(NavigateEvent.NavigateBack()))
        })
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            wallet: Wallet,
            initAccountAsset: AccountAsset,
            onChainAddress: String?,
            satoshi: Long?,
        ): RecoverFundsViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            wallet: Wallet,
            initAccountAsset: AccountAsset,
            onChainAddress: String?,
            satoshi: Long?,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>
                ): T {
                    return assistedFactory.create(
                        wallet,
                        initAccountAsset,
                        onChainAddress,
                        satoshi
                    ) as T
                }
            }
    }
}
