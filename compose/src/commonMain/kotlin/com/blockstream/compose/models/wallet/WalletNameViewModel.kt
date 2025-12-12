package com.blockstream.compose.models.wallet

import androidx.lifecycle.viewModelScope
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.cleanup
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.extensions.previewWallet
import com.blockstream.compose.events.Event
import com.blockstream.compose.events.Events
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.sideeffects.SideEffects
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

abstract class WalletNameViewModelAbstract(greenWallet: GreenWallet) :
    GreenViewModel(greenWalletOrNull = greenWallet) {
    override fun screenName(): String = "RenameWallet"

    override val isLoginRequired: Boolean = false
    abstract val name: MutableStateFlow<String>
}

class WalletNameViewModel(greenWallet: GreenWallet) : WalletNameViewModelAbstract(greenWallet = greenWallet) {
    override val name: MutableStateFlow<String> = MutableStateFlow(greenWallet.name)

    init {
        bootstrap()

        name.onEach {
            _isValid.value = it.cleanup().isNotBlank()
        }.launchIn(viewModelScope)
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)
        if (event is Events.Continue) {
            renameWallet()
        }
    }

    private fun renameWallet() {
        doAsync({
            name.value.cleanup().takeIf { it.isNotBlank() }?.also { name ->
                greenWallet.name = name
                database.updateWallet(greenWallet)
            } ?: throw Exception("Name should not be blank")
        }, onSuccess = {
            countly.renameWallet()
            postSideEffect(SideEffects.Dismiss)
        })
    }
}

class WalletNameViewModelPreview(
    greenWallet: GreenWallet
) : WalletNameViewModelAbstract(greenWallet = greenWallet) {

    override val name: MutableStateFlow<String> = MutableStateFlow(greenWallet.name)

    companion object {
        fun preview() = WalletNameViewModelPreview(
            previewWallet()
        )
    }
}