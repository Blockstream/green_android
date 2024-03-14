package com.blockstream.common.models.receive

import com.blockstream.common.data.Denomination
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.extensions.isPolicyAsset
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.UserInput
import com.blockstream.common.utils.getBitcoinOrLiquidUnit
import com.blockstream.common.utils.getFiatCurrency
import com.blockstream.common.utils.toAmountLook
import com.rickclephas.kmm.viewmodel.coroutineScope
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


abstract class RequestAmountViewModelAbstract(
    greenWallet: GreenWallet,
    accountAssetOrNull: AccountAsset?
) :
    GreenViewModel(greenWalletOrNull = greenWallet, accountAssetOrNull = accountAssetOrNull) {

    override fun screenName(): String = "RequestAmount"

    override fun segmentation(): HashMap<String, Any>? =
        countly.accountSegmentation(session, account)

    abstract val isPolicyAsset: Boolean

    @NativeCoroutinesState
    abstract val amount: MutableStateFlow<String>

    @NativeCoroutinesState
    abstract val amountCurrency: MutableStateFlow<String>

    @NativeCoroutinesState
    abstract val exchange: MutableStateFlow<String>

    @NativeCoroutinesState
    abstract val isFiat: MutableStateFlow<Boolean>
}

class RequestAmountViewModel(
    greenWallet: GreenWallet,
    accountAsset: AccountAsset,
    val initialAmount: String
) : RequestAmountViewModelAbstract(greenWallet = greenWallet, accountAssetOrNull = accountAsset) {

    override val isPolicyAsset = accountAsset.asset.assetId.isPolicyAsset(account.network)

    override val amount = MutableStateFlow(initialAmount)

    override val amountCurrency = MutableStateFlow("")
    override val exchange = MutableStateFlow("")
    override val isFiat = MutableStateFlow(false)

    class LocalEvents {
        object ToggleCurrency : Event
    }

    init {
        if (initialAmount.isNotBlank()) {
            viewModelScope.coroutineScope.launch {
                amount.value = (if (isPolicyAsset) {
                    try {
                        // Amount is always in BTC value, convert it to user's settings
                        session.convert(
                            assetId = accountAsset.account.network.policyAsset,
                            asString = initialAmount
                        )?.toAmountLook(
                            session,
                            withUnit = false,
                            withGrouping = false,
                            withMinimumDigits = false
                        ) ?: initialAmount
                    } catch (e: Exception) {
                        e.printStackTrace()
                        initialAmount
                    }
                } else {
                    initialAmount
                })
            }
        }

        if (isPolicyAsset) {
            amount
                .debounce(10)
                .onEach {
                    updateExchange()
                }
                .launchIn(viewModelScope.coroutineScope)
        }

        isFiat
            .onEach {
                amountCurrency.value = if (it) {
                    getFiatCurrency(session)
                } else if (accountAsset.assetId.isPolicyAsset(accountAsset.account.network)) {
                    getBitcoinOrLiquidUnit(
                        session = session,
                        assetId = accountAsset.account.network.policyAsset
                    )
                } else {
                    accountAsset.asset.ticker ?: ""
                }
            }.launchIn(viewModelScope.coroutineScope)

        bootstrap()
    }

    override fun handleEvent(event: Event) {
        super.handleEvent(event)

        if (event is LocalEvents.ToggleCurrency) {
            toggleCurrency()
        } else if (event is Events.Continue) {
            convertAmount()
        }
    }

    private fun convertAmount() {
        viewModelScope.coroutineScope.launch {
            val amount: String? = try {
                val input = UserInput.parseUserInput(
                    session = session,
                    input = amount.value,
                    assetId = accountAsset.value!!.assetId,
                    denomination = Denomination.defaultOrFiat(session, isFiat.value)
                )

                // Convert it to BTC as per BIP21 spec
                input.getBalance().let { balance ->
                    if (balance != null && balance.satoshi > 0) {
                        balance.valueInMainUnit.let {
                            // Remove trailing zeros if needed
                            if (it.contains(".")) it.replace("0*$".toRegex(), "")
                                .replace("\\.$".toRegex(), "") else it
                        }
                    } else {
                        null
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

            postSideEffect(SideEffects.Success(amount))
            postSideEffect(SideEffects.Dismiss)
        }
    }

    private fun updateExchange() {
        amount.value.let { amount ->
            // Convert between BTC / Fiat
            doAsync({
                val isFiat = isFiat.value ?: false

                UserInput.parseUserInput(
                    session,
                    amount,
                    assetId = accountAsset.value!!.assetId,
                    denomination = Denomination.defaultOrFiat(session, isFiat)
                ).getBalance()?.let {
                    "≈ " + it.toAmountLook(
                        session = session,
                        assetId = accountAsset.value!!.assetId,
                        denomination = Denomination.defaultOrFiat(session, !isFiat),
                        withUnit = true,
                        withGrouping = true,
                        withMinimumDigits = false
                    )
                } ?: ""
            }, preAction = null, postAction = null, onSuccess = {
                exchange.value = it
            }, onError = {
                exchange.value = ""
            })
        }
    }

    private fun toggleCurrency() {
        viewModelScope.coroutineScope.launch {

            val isFiat = isFiat.value ?: false

            // Toggle it first as the amount trigger will be called with wrong isFiat value
            this@RequestAmountViewModel.isFiat.value = !isFiat

            // Convert between BTC / Fiat
            amount.value = try {
                UserInput.parseUserInput(
                    session,
                    amount.value,
                    assetId = accountAsset.value!!.assetId,
                    denomination = Denomination.defaultOrFiat(session, isFiat)
                ).getBalance()?.let {
                    if (it.satoshi > 0) {
                        it.toAmountLook(
                            session = session,
                            assetId = accountAsset.value!!.assetId,
                            denomination = Denomination.defaultOrFiat(session, !isFiat),
                            withUnit = false,
                            withGrouping = false,
                            withMinimumDigits = false
                        )
                    } else {
                        ""
                    }
                } ?: ""
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }
        }
    }

}