package com.blockstream.common.models.promo

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.x
import com.blockstream.common.data.GreenWallet
import com.blockstream.ui.navigation.NavAction
import com.blockstream.ui.navigation.NavData
import com.blockstream.common.data.Promo
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.models.GreenViewModel
import com.rickclephas.kmp.observableviewmodel.launch

abstract class PromoViewModelAbstract(promo: Promo, greenWalletOrNull: GreenWallet?) : GreenViewModel(
    greenWalletOrNull = greenWalletOrNull
) {
    override fun screenName(): String = "Promo"

    init {
        this.promo.value = promo
    }

    override fun initPromo() {}
}

class PromoViewModel(promo: Promo, greenWalletOrNull: GreenWallet?) :
    PromoViewModelAbstract(greenWalletOrNull = greenWalletOrNull, promo = promo) {

    init {
        viewModelScope.launch {
            _navData.value = NavData(title = promo.title,
                showNavigationIcon = promo.layoutLarge == 0,
                actions = listOfNotNull(
                    (NavAction(
                        title = "Close",
                        icon = Res.drawable.x,
                        isMenuEntry = false
                    ) {
                        postEvent(Events.NavigateBack)
                    }).takeIf { promo.layoutLarge == 1 }
                ))
        }

        bootstrap()
    }
}

class PromoViewModelPreview() :
    PromoViewModelAbstract(promo = Promo.preview1, greenWalletOrNull = previewWallet()) {

    companion object {
        fun preview() = PromoViewModelPreview()
    }
}