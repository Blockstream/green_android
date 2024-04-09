package com.blockstream.common.models.send

import com.blockstream.common.data.DenominatedValue
import com.blockstream.common.data.Denomination
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.NavData
import com.blockstream.common.extensions.ifConnected
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.utils.Loggable
import com.rickclephas.kmm.viewmodel.coroutineScope
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class DenominationViewModelAbstract(
    greenWallet: GreenWallet,
    denominatedValue: DenominatedValue
) : GreenViewModel(greenWalletOrNull = greenWallet) {
    override fun screenName(): String = "Denomination"

    override fun segmentation(): HashMap<String, Any>? {
        return countly.sessionSegmentation(session = session)
    }

    @NativeCoroutinesState
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
        _navData.value = NavData(
            title = "id_enter_amount_in"
        )

        _denomination.value = denominatedValue.denomination

        session.ifConnected {
            viewModelScope.coroutineScope.launch {
                _denominations.value = listOfNotNull(
                    DenominatedValue.toDenomination(denominatedValue = denominatedValue, denomination = Denomination.BTC, session = session),
                    DenominatedValue.toDenomination(denominatedValue = denominatedValue, denomination = Denomination.MBTC, session = session),
                    DenominatedValue.toDenomination(denominatedValue = denominatedValue, denomination = Denomination.UBTC, session = session),
                    DenominatedValue.toDenomination(denominatedValue = denominatedValue, denomination = Denomination.BITS, session = session),
                    DenominatedValue.toDenomination(denominatedValue = denominatedValue, denomination = Denomination.SATOSHI, session = session),

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