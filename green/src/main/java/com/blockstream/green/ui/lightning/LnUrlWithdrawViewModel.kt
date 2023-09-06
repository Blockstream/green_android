package com.blockstream.green.ui.lightning

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import breez_sdk.LnUrlWithdrawRequestData
import breez_sdk.LnUrlWithdrawResult
import com.blockstream.common.data.DenominatedValue
import com.blockstream.common.data.Denomination
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.lightning.maxReceivableSatoshi
import com.blockstream.common.lightning.maxWithdrawableSatoshi
import com.blockstream.common.lightning.minWithdrawableSatoshi
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.UserInput
import com.blockstream.green.extensions.boolean
import com.blockstream.green.extensions.string
import com.blockstream.green.ui.bottomsheets.DenominationListener
import com.blockstream.green.ui.wallet.AbstractAccountWalletViewModel
import com.blockstream.green.utils.toAmountLook
import com.blockstream.green.utils.toAmountLookOrNa
import com.rickclephas.kmm.viewmodel.coroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import java.lang.Long.min

@KoinViewModel
class LnUrlWithdrawViewModel constructor(
    @InjectedParam wallet: GreenWallet,
    @InjectedParam val accountAsset: AccountAsset,
    @InjectedParam val requestData: LnUrlWithdrawRequestData,
) : AbstractAccountWalletViewModel(
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
            viewModelScope.coroutineScope.launch {
                amount.value = requestData.minWithdrawableSatoshi().toAmountLook(
                    session = session,
                    assetId = accountValue.network.policyAsset,
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
            .launchIn(viewModelScope.coroutineScope)

        denomination.asFlow()
            .onEach {
                amountCurrency.value = it.unit(session, accountValue.network.policyAsset)
            }.launchIn(viewModelScope.coroutineScope)

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

        }.launchIn(viewModelScope.coroutineScope)
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
        val balance = session.accountAssets(account = accountValue).value.policyAsset

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
                if (it is LnUrlWithdrawResult.ErrorStatus) {
                    throw Exception(it.data.reason)
                }
            }
        }, onSuccess = {
            postSideEffect(SideEffects.Success())
        })
    }

    override fun setDenomination(denominatedValue: DenominatedValue) {
        amount.value = denominatedValue.asInput(session) ?: ""
        denomination.value = denominatedValue.denomination
    }
}
