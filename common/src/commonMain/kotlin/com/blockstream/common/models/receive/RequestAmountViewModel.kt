package com.blockstream.common.models.receive

import androidx.lifecycle.viewModelScope
import com.blockstream.common.data.Denomination
import com.blockstream.common.data.GreenWallet
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
import com.blockstream.ui.events.Event
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

abstract class RequestAmountViewModelAbstract(
    greenWallet: GreenWallet,
    accountAsset: AccountAsset
) : GreenViewModel(greenWalletOrNull = greenWallet, accountAssetOrNull = accountAsset) {

    override fun screenName(): String = "RequestAmount"

    override fun segmentation(): HashMap<String, Any>? =
        countly.accountSegmentation(session, account)

    abstract val isPolicyAsset: Boolean
    abstract val amount: MutableStateFlow<String>
    abstract val amountCurrency: MutableStateFlow<String>
    abstract val exchange: MutableStateFlow<String>
}

class RequestAmountViewModel(
    greenWallet: GreenWallet,
    accountAsset: AccountAsset,
    val initialAmount: String
) : RequestAmountViewModelAbstract(greenWallet = greenWallet, accountAsset = accountAsset) {

    override val isPolicyAsset = accountAsset.asset.assetId.isPolicyAsset(account.network)

    override val amount = MutableStateFlow(initialAmount)

    override val amountCurrency = MutableStateFlow("")
    override val exchange = MutableStateFlow("")

    class LocalEvents {
        object ToggleCurrency : Event
    }

    init {
        if (initialAmount.isNotBlank()) {
            viewModelScope.launch {
                amount.value = (if (isPolicyAsset) {
                    try {
                        // Amount is always in BTC value, convert it to user's settings
                        session.convert(
                            assetId = accountAsset.account.network.policyAsset,
                            asString = initialAmount
                        )?.toAmountLook(
                            session = session,
                            withUnit = false,
                            withGrouping = false,
                            withMinimumDigits = false,
                            denomination = denomination.value
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
            combine(denomination, amount.debounce(10)) { _, _ ->
                updateExchange()
            }.launchIn(viewModelScope)
        }

        denomination
            .onEach {
                amountCurrency.value = if (it.isFiat) {
                    getFiatCurrency(session)
                } else if (accountAsset.assetId.isPolicyAsset(accountAsset.account.network)) {
                    getBitcoinOrLiquidUnit(
                        session = session,
                        assetId = accountAsset.account.network.policyAsset,
                        denomination = denomination.value
                    )
                } else {
                    accountAsset.asset.ticker ?: ""
                }
            }.launchIn(viewModelScope)

        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        if (event is LocalEvents.ToggleCurrency) {
            toggleCurrency()
        } else if (event is Events.Continue) {
            convertAmount()
        }
    }

    private fun convertAmount() {
        viewModelScope.launch {
            val amount: String? = try {
                val input = UserInput.parseUserInput(
                    session = session,
                    input = amount.value,
                    assetId = accountAsset.value!!.assetId,
                    denomination = _denomination.value
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
        // Convert between BTC / Fiat
        doAsync({
            UserInput.parseUserInput(
                session = session,
                input = amount.value,
                assetId = accountAsset.value!!.assetId,
                denomination = _denomination.value
            ).getBalance()?.let {
                "≈ " + it.toAmountLook(
                    session = session,
                    assetId = accountAsset.value!!.assetId,
                    denomination = Denomination.exchange(session, denomination.value),
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

    private fun toggleCurrency() {
        viewModelScope.launch {

            // Convert between BTC / Fiat
            amount.value = try {
                UserInput.parseUserInput(
                    session,
                    amount.value,
                    assetId = accountAsset.value!!.assetId,
                    denomination = _denomination.value
                ).getBalance()?.let {
                    if (it.satoshi > 0) {
                        it.toAmountLook(
                            session = session,
                            assetId = accountAsset.value!!.assetId,
                            denomination = Denomination.exchange(session, _denomination.value),
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
            } finally {
                _denomination.value =
                    denomination.value.let { Denomination.exchange(session, it) ?: it }
            }
        }
    }

}