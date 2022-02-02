package com.blockstream.green.ui.receive

import androidx.lifecycle.*
import com.blockstream.gdk.params.Convert
import com.blockstream.green.gdk.GreenSession
import com.blockstream.green.gdk.observable
import com.blockstream.green.ui.AppViewModel
import com.blockstream.green.utils.UserInput
import com.blockstream.green.utils.getBitcoinOrLiquidUnit
import com.blockstream.green.utils.getFiatCurrency
import com.blockstream.green.utils.toAmountLook
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


class RequestAmountLabelViewModel @AssistedInject constructor(
    @Assisted val session: GreenSession,
    @Assisted("initialRequestAmount") val initialRequestAmount: String?,
    @Assisted("initialLabel") val initialLabel: String?,
) : AppViewModel() {

    var requestAmount: MutableLiveData<String> =
        MutableLiveData(initialRequestAmount?.let { amount ->
            try {
                // Amount is always in BTC value, convert it to user's settings
                session
                    .convertAmount(Convert.forUnit(amount = amount))
                    ?.toAmountLook(
                        session,
                        withUnit = false,
                        withGrouping = false,
                        withMinimumDigits = false
                    )!!

            } catch (e: Exception) {
                e.printStackTrace()
                amount
            }
        })

    var amountCurrency = MutableLiveData<String>()
    var exchange = MutableLiveData("")

    var isFiat = MutableLiveData(false)
    var label = MutableLiveData<String?>(initialLabel)

    init {
        requestAmount
            .asFlow()
            .debounce(10)
            .onEach {
                updateExchange()
            }
            .launchIn(viewModelScope)

        isFiat.observe(lifecycleOwner){ isFiat ->
            (if(isFiat) getFiatCurrency(session) else getBitcoinOrLiquidUnit(session)).let {
                amountCurrency.value = it
            }
        }
    }

    private fun updateExchange() {
        requestAmount.value?.let { amount ->
            // Convert between BTC / Fiat
            session.observable {
                val isFiat = isFiat.value ?: false

                UserInput.parseUserInput(
                    session,
                    amount,
                    isFiat = isFiat
                ).getBalance(session)?.let {
                    "≈ " + it.toAmountLook(
                        session = session,
                        isFiat = !isFiat,
                        withUnit = true,
                        withGrouping = true,
                        withMinimumDigits = false
                    )
                } ?: ""

            }.subscribeBy(
                onSuccess = {
                    exchange.postValue(it)
                },
                onError = {
                    it.printStackTrace()
                    exchange.postValue("")
                }
            )
        }
    }

    fun toggleCurrency() {

        val isFiat = isFiat.value ?: false

        // Toggle it first as the amount trigger will be called with wrong isFiat value
        this.isFiat.value = !isFiat

        // Convert between BTC / Fiat
        requestAmount.value = try {
            val input =
                UserInput.parseUserInput(session, requestAmount.value, isFiat = isFiat)
            input.getBalance(session)?.let {
                if (it.satoshi > 0) {
                    it.toAmountLook(
                        session = session,
                        isFiat = !isFiat,
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

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            session: GreenSession,
            @Assisted("initialRequestAmount")
            initialRequestAmount: String?,
            @Assisted("initialLabel")
            initialLabel: String?
        ): RequestAmountLabelViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            session: GreenSession,
            initialRequestAmount: String?,
            initialLabel: String?
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(session, initialRequestAmount, initialLabel) as T
            }
        }
    }
}