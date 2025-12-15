package com.blockstream.compose.models.send

import androidx.lifecycle.viewModelScope
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_enter_amount_in
import com.blockstream.data.data.DenominatedValue
import com.blockstream.data.data.Denomination
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.extensions.ifConnected
import com.blockstream.compose.extensions.previewWallet
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.navigation.NavData
import com.blockstream.utils.Loggable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

abstract class DenominationViewModelAbstract(
    greenWallet: GreenWallet,
    denominatedValue: DenominatedValue
) : GreenViewModel(greenWalletOrNull = greenWallet) {
    override fun screenName(): String = "Denomination"

    override fun segmentation(): HashMap<String, Any>? {
        return countly.sessionSegmentation(session = session)
    }
    abstract val denominations: StateFlow<List<DenominatedValue>>
}

class DenominationViewModel(
    greenWallet: GreenWallet,
    denominatedValue: DenominatedValue
) : DenominationViewModelAbstract(greenWallet = greenWallet, denominatedValue = denominatedValue) {

    private val _denominations: MutableStateFlow<List<DenominatedValue>> =
        MutableStateFlow(listOf())
    override val denominations: StateFlow<List<DenominatedValue>> = _denominations.asStateFlow()

    init {

        viewModelScope.launch {
            _navData.value = NavData(
                title = getString(Res.string.id_enter_amount_in)
            )
        }

        _denomination.value = denominatedValue.denomination

        session.ifConnected {
            viewModelScope.launch {
                _denominations.value = listOfNotNull(
                    DenominatedValue.toDenomination(
                        denominatedValue = denominatedValue,
                        denomination = Denomination.BTC,
                        session = session
                    ),
                    DenominatedValue.toDenomination(
                        denominatedValue = denominatedValue,
                        denomination = Denomination.MBTC,
                        session = session
                    ),
                    DenominatedValue.toDenomination(
                        denominatedValue = denominatedValue,
                        denomination = Denomination.UBTC,
                        session = session
                    ),
                    DenominatedValue.toDenomination(
                        denominatedValue = denominatedValue,
                        denomination = Denomination.BITS,
                        session = session
                    ),
                    DenominatedValue.toDenomination(
                        denominatedValue = denominatedValue,
                        denomination = Denomination.SATOSHI,
                        session = session
                    ),

                    session.getSettings()?.pricing?.currency?.let {
                        DenominatedValue.toDenomination(
                            denominatedValue = denominatedValue,
                            denomination = Denomination.FIAT(it),
                            session = session
                        )
                    }
                )
            }
        }

        bootstrap()
    }

    companion object : Loggable()
}

class DenominationViewModelPreview(greenWallet: GreenWallet) :
    DenominationViewModelAbstract(greenWallet = greenWallet, denominatedValue = DenominatedValue(denomination = Denomination.BTC)) {

    override val denominations: StateFlow<List<DenominatedValue>> =
        MutableStateFlow(
            listOf(
                DenominatedValue(denomination = Denomination.BTC, asLook = "1 BTC"),
                DenominatedValue(denomination = Denomination.MBTC, asLook = "1 BTC"),
                DenominatedValue(denomination = Denomination.UBTC, asLook = "1 BTC"),
                DenominatedValue(denomination = Denomination.BITS, asLook = "1 BTC"),
                DenominatedValue(denomination = Denomination.SATOSHI, asLook = "1 sats")
            )
        )

    companion object {
        fun preview() = DenominationViewModelPreview(previewWallet())
    }
}