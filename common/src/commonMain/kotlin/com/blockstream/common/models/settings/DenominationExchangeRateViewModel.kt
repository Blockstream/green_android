package com.blockstream.common.models.settings

import com.blockstream.common.BTC_UNIT
import com.blockstream.common.BitcoinUnits
import com.blockstream.common.TestnetUnits
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.WalletExtras
import com.blockstream.common.events.Event
import com.blockstream.common.extensions.ifConnected
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.Settings
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffects
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach


abstract class DenominationExchangeRateViewModelAbstract(greenWallet: GreenWallet) :
    GreenViewModel(greenWalletOrNull = greenWallet) {
    override fun screenName(): String = "DenominationAndExchangeRate"

    abstract val units: List<String>

    @NativeCoroutinesState
    abstract val selectedUnit: StateFlow<String>

    @NativeCoroutinesState
    abstract val exchangeAndCurrencies: StateFlow<List<String>>

    @NativeCoroutinesState
    abstract val selectedExchangeAndCurrency: StateFlow<String>
}

class DenominationExchangeRateViewModel(greenWallet: GreenWallet) :
    DenominationExchangeRateViewModelAbstract(greenWallet = greenWallet) {

    override val units: List<String> = if (greenWallet.isTestnet) TestnetUnits else BitcoinUnits

    private val _selectedUnit: MutableStateFlow<String> = MutableStateFlow("")
    override val selectedUnit: StateFlow<String> = _selectedUnit.asStateFlow()

    private val _exchangeAndCurrencies: MutableStateFlow<List<String>> = MutableStateFlow(listOf())
    override val exchangeAndCurrencies = _exchangeAndCurrencies.asStateFlow()

    private val _selectedExchangeAndCurrency: MutableStateFlow<String> = MutableStateFlow("")
    override val selectedExchangeAndCurrency: StateFlow<String> =
        _selectedExchangeAndCurrency.asStateFlow()

    private val availablePricing = session.ifConnected { session.availableCurrencies() } ?: listOf()

    class LocalEvents {
        data class Set(val unit: String? = null, val exchangeAndCurrency: String? = null) : Event
        object Save : Event
    }

    init {
        session.ifConnected {
            session.settings().filterNotNull().onEach { settings ->
                _selectedUnit.value = settings.networkUnit(session)

                _exchangeAndCurrencies.value = availablePricing.map {
                    "id_s_from_s|${it.currency}|${it.exchange}"
                }

                _selectedExchangeAndCurrency.value = settings.pricing.let {
                    "id_s_from_s|${it.currency}|${it.exchange}"
                }
            }.launchIn(this)
        }

        bootstrap()
    }

    override fun handleEvent(event: Event) {
        super.handleEvent(event)

        if (event is LocalEvents.Set) {
            event.unit?.also {
                _selectedUnit.value = it
            }
            event.exchangeAndCurrency?.also {
                _selectedExchangeAndCurrency.value = it
            }
        } else if (event is LocalEvents.Save) {
            saveSettings()
        }
    }

    private fun saveSettings() {
        val newSettings = session.getSettings()!!.let { settings ->
            settings.copy(
                unit = Settings.fromNetworkUnit(selectedUnit.value, session),
                pricing = selectedExchangeAndCurrency.value.let {
                    _exchangeAndCurrencies.value.indexOf(it).takeIf { it >= 0 }?.let {
                        availablePricing[it]
                    } ?: settings.pricing
                }
            )
        }

        doAsync({
            session.changeGlobalSettings(newSettings)
            if (!greenWallet.isEphemeral) {
                greenWallet.also {
                    // Pass settings to Lightning Shortcut
                    sessionManager.getWalletSessionOrNull(it.lightningShortcutWallet())
                        ?.also { lightningSession ->
                            lightningSession.changeGlobalSettings(newSettings)
                        }

                    it.extras = WalletExtras(settings = newSettings.forWalletExtras())

                    database.updateWallet(it)
                }
            }
        }, onSuccess = {
            postSideEffect(SideEffects.Dismiss)
        })
    }
}

class DenominationExchangeRateViewModelPreview(greenWallet: GreenWallet) :
    DenominationExchangeRateViewModelAbstract(greenWallet = greenWallet) {
    companion object {
        fun preview() = DenominationExchangeRateViewModelPreview(previewWallet(isHardware = false))
    }

    override val units: List<String> = BitcoinUnits

    override val selectedUnit: StateFlow<String> = MutableStateFlow(BTC_UNIT)

    override val exchangeAndCurrencies: StateFlow<List<String>> = MutableStateFlow(listOf("EUR from BITFINEX", "USD from COINGECKO"))

    override val selectedExchangeAndCurrency: StateFlow<String> = MutableStateFlow("EUR from BITFINEX")
}