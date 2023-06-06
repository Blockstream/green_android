package com.blockstream.green.ui.lightning

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import breez_sdk.LnUrlCallbackStatus
import breez_sdk.LnUrlWithdrawRequestData
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.green.data.Countly
import com.blockstream.green.data.DenominatedValue
import com.blockstream.green.data.Denomination
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.extensions.boolean
import com.blockstream.green.extensions.string
import com.blockstream.green.gdk.policyAsset
import com.blockstream.green.managers.SessionManager
import com.blockstream.green.ui.bottomsheets.DenominationListener
import com.blockstream.green.ui.wallet.AbstractAccountWalletViewModel
import com.blockstream.green.utils.ConsumableEvent
import com.blockstream.green.utils.UserInput
import com.blockstream.green.utils.toAmountLook
import com.blockstream.green.utils.toAmountLookOrNa
import com.blockstream.lightning.maxReceivableSatoshi
import com.blockstream.lightning.maxWithdrawableSatoshi
import com.blockstream.lightning.minWithdrawableSatoshi
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.lang.Long.min


class LnUrlWithdrawViewModel @AssistedInject constructor(
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    countly: Countly,
    @Assisted wallet: Wallet,
    @Assisted val accountAsset: AccountAsset,
    @Assisted val requestData: LnUrlWithdrawRequestData,
) : AbstractAccountWalletViewModel(
    sessionManager,
    walletRepository,
    countly,
    wallet,
    accountAsset.account
), DenominationListener {
    val minSatoshi: MutableLiveData<Long> = MutableLiveData(requestData.minWithdrawableSatoshi())
    var minWithdrawAmount: MutableLiveData<String> = MutableLiveData("")

    val maxSatoshi: MutableLiveData<Long> = MutableLiveData(requestData.maxWithdrawableSatoshi())
    var maxWithdrawAmount: MutableLiveData<String> = MutableLiveData("")

    val amountIsLocked = MutableLiveData(requestData.minWithdrawableSatoshi() == requestData.maxWithdrawableSatoshi())

    val amount = MutableLiveData("")
    val amountCurrency = MutableLiveData("")
    val denomination = MutableLiveData(Denomination.default(session))
    val exchange = MutableLiveData("")

    private suspend fun amountInSatoshi() =
        if (amountIsLocked.boolean()) requestData.maxWithdrawableSatoshi() else UserInput.parseUserInputSafe(
            session = session,
            input = amount.value,
            assetId = accountAsset.assetId,
            denomination = denomination.value
        ).getBalance()?.satoshi ?: 0

    val description = MutableLiveData(requestData.defaultDescription)

    var error : MutableLiveData<String> = MutableLiveData(null)

    init {

        // Set amount if min/max is the same
        if(amountIsLocked.boolean()){
            viewModelScope.launch {
                amount.value = requestData.minWithdrawableSatoshi().toAmountLook(
                    session = session,
                    assetId = account.network.policyAsset,
                    denomination = denomination.value,
                    withUnit = false,
                    withGrouping = false
                )
            }
        }

        amount
            .asFlow()
            .onEach {
                updateExchange()
                check()
            }
            .launchIn(viewModelScope)

        denomination.asFlow()
            .onEach {
                amountCurrency.value = it.unit(session, account.network.policyAsset)
            }.launchIn(viewModelScope)

        combine(session.lightningNodeInfoStateFlow, denomination.asFlow()) { nodeState, _ ->
            nodeState
        }.onEach {
            // Min
            min(
                it.maxReceivableSatoshi(),
                requestData.minWithdrawableSatoshi()
            ).also { minWithdrawable ->
                minSatoshi.value = minWithdrawable
                minWithdrawAmount.value = minWithdrawable.toAmountLookOrNa(session = session, withUnit = false, denomination = denomination.value)
            }

            // Max
            min(
                it.maxReceivableSatoshi(),
                requestData.maxWithdrawableSatoshi()
            ).also { maxWithdrawable ->
                maxSatoshi.value = maxWithdrawable
                maxWithdrawAmount.value = maxWithdrawable.toAmountLookOrNa(session = session, denomination = denomination.value)

            }

        }.launchIn(viewModelScope)
    }

    private fun updateExchange() {
        amount.value?.let { amount ->
            doUserAction({
                UserInput.parseUserInput(
                    session,
                    amount,
                    assetId = accountAsset.assetId,
                    denomination = denomination.value
                ).toAmountLookOrEmpty(
                    denomination = Denomination.exchange(session = session, denomination = denomination.value),
                    withUnit = true,
                    withGrouping = true,
                    withMinimumDigits = false).let {
                    if(it.isNotBlank()) "≈ $it" else it
                }
            }, preAction = null, postAction = null, onSuccess = {
                exchange.postValue(it)
            }, onError = {
                exchange.postValue("")
            })
        }
    }

    private suspend fun check(){
        val satoshi = amountInSatoshi()

        val min = minSatoshi.value ?: 0
        val max = maxSatoshi.value ?: 0
        val balance = session.accountAssets(account = account).policyAsset()

        error.value = if (satoshi <= 0L) {
            "id_invalid_amount"
        } else if (satoshi < min) {
            "id_amount_must_be_at_least_s|${min.toAmountLook(session = session, denomination = denomination.value)}"
        } else if (satoshi > balance) {
            "id_insufficient_funds"
        } else if (satoshi > max) {
            "id_amount_must_be_at_most_s|${max.toAmountLook(session = session, denomination = denomination.value)}"
        } else {
            null
        }
    }

    fun withdraw() {
        doUserAction({
            session.lightningSdk.withdrawLnurl(requestData = requestData, amountInSatoshi(), description.string()).also {
                if (it is LnUrlCallbackStatus.ErrorStatus) {
                    throw Exception(it.data.reason)
                }
            }
        }, onSuccess = {
            onEvent.postValue(ConsumableEvent(NavigateEvent.NavigateBack()))
        })
    }

    override fun setDenomination(denominatedValue: DenominatedValue) {
        amount.value = denominatedValue.asInput(session) ?: ""
        denomination.value = denominatedValue.denomination
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            wallet: Wallet,
            accountAsset: AccountAsset,
            requestData: LnUrlWithdrawRequestData
        ): LnUrlWithdrawViewModel
    }

    companion object {

        fun provideFactory(
            assistedFactory: AssistedFactory,
            wallet: Wallet,
            accountAsset: AccountAsset,
            requestData: LnUrlWithdrawRequestData
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                ): T {
                    return assistedFactory.create(
                        wallet,
                        accountAsset,
                        requestData
                    ) as T
                }
            }
    }
}
