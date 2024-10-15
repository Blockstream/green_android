package com.blockstream.common.models.promo

import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.NavData
import com.blockstream.common.data.Promo
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.models.GreenViewModel
import com.rickclephas.kmp.observableviewmodel.launch

abstract class PromoViewModelAbstract(greenWallet: GreenWallet, promo: Promo) : GreenViewModel(
    greenWalletOrNull = greenWallet
) {
    override fun screenName(): String = "Promo"

    init {
        this.promo.value = promo
    }

    override fun initPromo() { }
}

class PromoViewModel(greenWallet: GreenWallet, promo: Promo) :
    PromoViewModelAbstract(greenWallet = greenWallet, promo = promo) {

    init {
        viewModelScope.launch {
            _navData.value = NavData(title = promo.title)
        }

        bootstrap()
    }
}

class PromoViewModelPreview() :
    PromoViewModelAbstract(greenWallet = previewWallet(), promo = Promo.preview1) {

    companion object {
        fun preview() = PromoViewModelPreview()
    }
}