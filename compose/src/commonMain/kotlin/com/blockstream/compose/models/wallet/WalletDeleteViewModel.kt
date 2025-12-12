package com.blockstream.compose.models.wallet

import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.previewWallet
import com.blockstream.compose.events.Event
import com.blockstream.compose.events.Events
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.sideeffects.SideEffects

abstract class WalletDeleteViewModelAbstract(greenWallet: GreenWallet) :
    GreenViewModel(greenWalletOrNull = greenWallet) {
    override fun screenName(): String = "DeleteWallet"

    override val isLoginRequired: Boolean = false
}

class WalletDeleteViewModel(greenWallet: GreenWallet) : WalletDeleteViewModelAbstract(greenWallet = greenWallet) {
    init {
        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)
        if (event is Events.Continue) {
            deleteWallet()
        }
    }

    private fun deleteWallet() {
        doAsync(action = {
            sessionManager.destroyWalletSession(greenWallet)
            database.deleteWallet(greenWallet.id)
        }, onSuccess = {
            countly.deleteWallet()
            postSideEffect(SideEffects.Dismiss)
        })
    }
}

class WalletDeleteViewModelPreview(
    greenWallet: GreenWallet
) : WalletDeleteViewModelAbstract(greenWallet = greenWallet) {

    companion object {
        fun preview() = WalletDeleteViewModelPreview(
            previewWallet()
        )
    }
}