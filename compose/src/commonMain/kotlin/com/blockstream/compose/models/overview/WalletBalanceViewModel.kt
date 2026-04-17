package com.blockstream.compose.models.overview

import androidx.lifecycle.viewModelScope
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.gdk.data.Assets
import com.blockstream.data.utils.getFiatCurrency
import com.blockstream.data.utils.userNumberFormat
import com.blockstream.compose.events.Event
import com.blockstream.compose.extensions.launchIn
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.models.IPostEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

interface IWalletBalance : IPostEvent {
    val hideAmounts: StateFlow<Boolean>
    val balancePrimary: StateFlow<String?>
}

open class WalletBalanceViewModel(greenWallet: GreenWallet) :
    GreenViewModel(greenWalletOrNull = greenWallet), IWalletBalance {

    override val hideAmounts: StateFlow<Boolean> = settingsManager.appSettingsStateFlow.map {
        it.hideAmounts
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        settingsManager.appSettings.hideAmounts
    )

    private val _balancePrimary: MutableStateFlow<String?> = MutableStateFlow(null)
    override val balancePrimary: StateFlow<String?> = _balancePrimary.asStateFlow()

    class LocalEvents {
        object ToggleHideAmounts : Event
    }

    override fun bootstrap() {
        super.bootstrap()

        combine(
            session.walletAssets,
            hideAmounts,
            session.settings()
        ) { walletAssets, _, _ ->
            walletAssets
        }.filter {
            session.isConnected
        }.onEach { walletAssetsState ->
            val assets = walletAssetsState.data()
            if (assets == null) {
                _balancePrimary.value = null
            } else {
                _balancePrimary.value = session.starsOrNull ?: buildFiatBalance(assets)
            }
        }.launchIn(this)
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is LocalEvents.ToggleHideAmounts -> {
                settingsManager.saveApplicationSettings(
                    settingsManager.getApplicationSettings().let {
                        it.copy(hideAmounts = !it.hideAmounts)
                    })

                if (settingsManager.appSettings.hideAmounts) {
                    countly.hideAmount(session)
                }
            }
        }
    }

    private suspend fun buildFiatBalance(assets: Assets): String {
        val fiatValues = mutableListOf<Double>()

        for ((assetId, satoshi) in assets.assets) {
            try {
                val balance = session.convert(assetId = assetId, asLong = satoshi)
                val fiatString = balance?.fiat ?: continue
                val fiatDouble = fiatString.toDoubleOrNull() ?: continue
                fiatValues.add(fiatDouble)
            } catch (_: Exception) {
            }
        }

        val fiatAmount = if (fiatValues.isNotEmpty()) fiatValues.sum() else null

        val currency = getFiatCurrency(session)

        if (fiatAmount != null) {
            val formatted = try {
                userNumberFormat(decimals = 2, withDecimalSeparator = true, withGrouping = true).format(fiatAmount)
            } catch (_: Exception) {
                null
            }
            return if (formatted != null) "$formatted $currency" else "-/- $currency"
        }

        return "-/- $currency"
    }
}
