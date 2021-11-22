package com.blockstream.green.ui.receive

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.blockstream.gdk.params.Convert
import com.blockstream.green.gdk.GreenSession
import com.blockstream.green.gdk.observable
import com.blockstream.green.ui.AppViewModel
import com.blockstream.green.utils.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.rxjava3.kotlin.subscribeBy


class RequestAmountLabelViewModel @AssistedInject constructor(
    @Assisted val session: GreenSession,
    @Assisted("initialRequestAmount") val initialRequestAmount: String?,
    @Assisted("initialLabel") val initialLabel: String?,
) : AppViewModel() {

    var requestAmount: MutableLiveData<String> =
        MutableLiveData(initialRequestAmount?.let { amount ->
            try {
                // Amount is always in BTC value, convert it to user's settings
                session.convertAmount(Convert.forUnit(amount = amount))
                    .btc(session, withUnit = false, withGrouping = false, withMinimumDigits = false)
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
        requestAmount.observe(viewLifecycleOwner){
            updateExchange()
        }

        isFiat.observe(viewLifecycleOwner){ isFiat ->
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
                    "≈ " + if (isFiat) {
                        it.btc(
                            session,
                            withUnit = true,
                            withGrouping = true,
                            withMinimumDigits = false
                        )
                    } else {
                        it.fiat(session, withUnit = true, withGrouping = true)
                    }
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
                    if (isFiat) {
                        it.btc(
                            session,
                            withUnit = false,
                            withGrouping = false,
                            withMinimumDigits = false
                        )
                    } else {
                        it.fiat(session, withUnit = false, withGrouping = false)
                    }
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