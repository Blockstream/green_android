package com.blockstream.common.models.wallet

import com.blockstream.common.data.GreenWallet
import com.blockstream.common.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.cleanup
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffects
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

abstract class WalletNameViewModelAbstract(greenWallet: GreenWallet) :
    GreenViewModel(greenWalletOrNull = greenWallet) {
    override fun screenName(): String = "RenameWallet"

    override val isLoginRequired: Boolean = false

    @NativeCoroutinesState
    abstract val name: MutableStateFlow<String>
}

class WalletNameViewModel(greenWallet: GreenWallet) : WalletNameViewModelAbstract(greenWallet = greenWallet) {
    override val name: MutableStateFlow<String> = MutableStateFlow(greenWallet.name)

    init {
        bootstrap()

        name.onEach {
            _isValid.value = it.cleanup().isNotBlank()
        }.launchIn(viewModelScope.coroutineScope)
    }

    override fun handleEvent(event: Event) {
        super.handleEvent(event)
        if (event is Events.Continue) {
            renameWallet()
        }
    }

    private fun renameWallet(){
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