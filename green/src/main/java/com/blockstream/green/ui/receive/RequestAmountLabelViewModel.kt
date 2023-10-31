package com.blockstream.green.ui.receive

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import com.blockstream.common.data.Denomination
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.isPolicyAsset
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.params.Convert
import com.blockstream.common.utils.UserInput
import com.blockstream.green.ui.wallet.AbstractAccountWalletViewModel
import com.blockstream.green.utils.getBitcoinOrLiquidUnit
import com.blockstream.green.utils.getFiatCurrency
import com.blockstream.green.utils.toAmountLook
import com.rickclephas.kmm.viewmodel.coroutineScope
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

@KoinViewModel
class RequestAmountLabelViewModel constructor(
    @InjectedParam wallet: GreenWallet,
    @InjectedParam val accountAssetValue: AccountAsset,
    @InjectedParam val initialRequestAmount: String?,
) : AbstractAccountWalletViewModel(wallet, accountAssetValue.account) {

    val isPolicyAsset = accountAssetValue.assetId.isPolicyAsset(accountValue.network)

    var requestAmount: MutableLiveData<String> =
        MutableLiveData(initialRequestAmount?.let { amount ->
            if(isPolicyAsset) {
                try {
                    runBlocking {
                        // Amount is always in BTC value, convert it to user's settings
                        session
                            .convertAmount(network, Convert.forUnit(amount = amount))
                            ?.toAmountLook(
                                session,
                                withUnit = false,
                                withGrouping = false,
                                withMinimumDigits = false
                            )!!
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    amount
                }
            }else{
                amount
            }
        })

    var amountCurrency = MutableLiveData<String>("")
    var exchange = MutableLiveData("")

    var isFiat = MutableLiveData(false)

    init {
        if (isPolicyAsset){
            requestAmount
                .asFlow()
                .debounce(10)
                .onEach {
                    updateExchange()
                }
                .launchIn(viewModelScope.coroutineScope)
        }

        isFiat.asFlow()
            .onEach {
                amountCurrency.value = if (it) {
                    getFiatCurrency(session)
                } else if (accountAssetValue.assetId.isPolicyAsset(accountAssetValue.account.network)) {
                    getBitcoinOrLiquidUnit(session = session, assetId = network.policyAsset)
                } else {
                    accountAssetValue.asset(session)?.ticker ?: ""
                }
            }.launchIn(viewModelScope.coroutineScope)
    }

    private fun updateExchange() {
        requestAmount.value?.let { amount ->
            // Convert between BTC / Fiat
            doUserAction({
                val isFiat = isFiat.value ?: false

                UserInput.parseUserInput(
                    session,
                    amount,
                    assetId = accountAssetValue.assetId,
                    denomination = Denomination.defaultOrFiat(session, isFiat)
                ).getBalance()?.let {
                    "≈ " + it.toAmountLook(
                        session = session,
                        assetId = accountAssetValue.assetId,
                        denomination = Denomination.defaultOrFiat(session, !isFiat),
                        withUnit = true,
                        withGrouping = true,
                        withMinimumDigits = false
                    )
                } ?: ""
            }, preAction = null, postAction = null, onSuccess = {
                exchange.postValue(it)
            }, onError = {
                exchange.postValue("")
            })
        }
    }

    fun toggleCurrency() {
        viewModelScope.coroutineScope.launch {

            val isFiat = isFiat.value ?: false

            // Toggle it first as the amount trigger will be called with wrong isFiat value
            this@RequestAmountLabelViewModel.isFiat.value = !isFiat

            // Convert between BTC / Fiat
            requestAmount.value = try {
                val input =
                    UserInput.parseUserInput(
                        session,
                        requestAmount.value,
                        assetId = accountAssetValue.assetId,
                        denomination = Denomination.defaultOrFiat(session, isFiat)
                    )
                input.getBalance()?.let {
                    if (it.satoshi > 0) {
                        it.toAmountLook(
                            session = session,
                            assetId = accountAssetValue.assetId,
                            denomination = Denomination.defaultOrFiat(session, !isFiat),
                            withUnit = false,
                            withGrouping = false,
                            withMinimumDigits = false
                        )
                    } else {
                        ""
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }
        }
    }
}