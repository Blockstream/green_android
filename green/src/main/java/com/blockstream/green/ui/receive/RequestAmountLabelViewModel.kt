package com.blockstream.green.ui.receive

import androidx.lifecycle.*
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.params.Convert
import com.blockstream.green.data.Countly
import com.blockstream.green.data.Denomination
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.gdk.asset
import com.blockstream.green.gdk.isPolicyAsset
import com.blockstream.green.managers.SessionManager
import com.blockstream.green.ui.wallet.AbstractAccountWalletViewModel
import com.blockstream.green.utils.UserInput
import com.blockstream.green.utils.getBitcoinOrLiquidUnit
import com.blockstream.green.utils.getFiatCurrency
import com.blockstream.green.utils.toAmountLook
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class RequestAmountLabelViewModel @AssistedInject constructor(
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    countly: Countly,
    @Assisted wallet: Wallet,
    @Assisted val accountAsset: AccountAsset,
    @Assisted("initialRequestAmount") val initialRequestAmount: String?,
) : AbstractAccountWalletViewModel(sessionManager, walletRepository, countly, wallet, accountAsset.account) {

    val isPolicyAsset = accountAsset.assetId.isPolicyAsset(account.network)

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
                .launchIn(viewModelScope)
        }

        isFiat.asFlow()
            .onEach {
                amountCurrency.value = if (it) {
                    getFiatCurrency(session)
                } else if (accountAsset.assetId.isPolicyAsset(accountAsset.account.network)) {
                    getBitcoinOrLiquidUnit(session = session, assetId = network.policyAsset)
                } else {
                    accountAsset.asset(session)?.ticker ?: ""
                }
            }.launchIn(viewModelScope)
    }

    private fun updateExchange() {
        requestAmount.value?.let { amount ->
            // Convert between BTC / Fiat
            doUserAction({
                val isFiat = isFiat.value ?: false

                UserInput.parseUserInput(
                    session,
                    amount,
                    assetId = accountAsset.assetId,
                    denomination = Denomination.defaultOrFiat(session, isFiat)
                ).getBalance()?.let {
                    "≈ " + it.toAmountLook(
                        session = session,
                        assetId = accountAsset.assetId,
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
        viewModelScope.launch {

            val isFiat = isFiat.value ?: false

            // Toggle it first as the amount trigger will be called with wrong isFiat value
            this@RequestAmountLabelViewModel.isFiat.value = !isFiat

            // Convert between BTC / Fiat
            requestAmount.value = try {
                val input =
                    UserInput.parseUserInput(
                        session,
                        requestAmount.value,
                        assetId = accountAsset.assetId,
                        denomination = Denomination.defaultOrFiat(session, isFiat)
                    )
                input.getBalance()?.let {
                    if (it.satoshi > 0) {
                        it.toAmountLook(
                            session = session,
                            assetId = accountAsset.assetId,
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

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            wallet: Wallet,
            accountAsset: AccountAsset,
            @Assisted("initialRequestAmount")
            initialRequestAmount: String?
        ): RequestAmountLabelViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            wallet: Wallet,
            accountAsset: AccountAsset,
            initialRequestAmount: String?
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(wallet, accountAsset, initialRequestAmount) as T
            }
        }
    }
}