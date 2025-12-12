package com.blockstream.compose.models.login

import androidx.lifecycle.viewModelScope
import com.blockstream.common.Urls
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.logException
import com.blockstream.common.extensions.previewWallet
import com.blockstream.compose.events.Event
import com.blockstream.compose.events.Events
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.sideeffects.SideEffect
import com.blockstream.compose.sideeffects.SideEffects
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

abstract class Bip39PassphraseViewModelAbstract(greenWallet: GreenWallet) :
    GreenViewModel(greenWalletOrNull = greenWallet) {
    override fun screenName(): String = "BIP39Passphrase"

    override val isLoginRequired: Boolean = false
    abstract val passphrase: MutableStateFlow<String>
    abstract val isAlwaysAsk: MutableStateFlow<Boolean>
}

class Bip39PassphraseViewModel(greenWallet: GreenWallet, passphrase: String) :
    Bip39PassphraseViewModelAbstract(greenWallet = greenWallet) {
    override val passphrase: MutableStateFlow<String> = MutableStateFlow(passphrase)
    override val isAlwaysAsk: MutableStateFlow<Boolean> =
        MutableStateFlow(greenWallet.askForBip39Passphrase)

    class LocalEvents {
        object LearnMore : Event
        object Clear : Event
    }

    class LocalSideEffects {
        data class SetBip39Passphrase(val passphrase: String) : SideEffect
    }

    init {
        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)
        when (event) {
            is Events.Continue -> {
                viewModelScope.launch(context = logException(countly)) {
                    greenWallet.askForBip39Passphrase = isAlwaysAsk.value
                    database.updateWallet(greenWallet)

                    postSideEffect(LocalSideEffects.SetBip39Passphrase(passphrase = passphrase.value))
                    postSideEffect(SideEffects.Dismiss)
                }
            }

            is LocalEvents.Clear -> {
                postSideEffect(LocalSideEffects.SetBip39Passphrase(passphrase = ""))
                postSideEffect(SideEffects.Dismiss)
            }

            is LocalEvents.LearnMore -> {
                postSideEffect(SideEffects.OpenBrowser(Urls.HELP_BIP39_PASSPHRASE))
            }
        }
    }
}

class Bip39PassphraseViewModelPreview(
    greenWallet: GreenWallet
) : Bip39PassphraseViewModelAbstract(greenWallet = greenWallet) {

    override val passphrase: MutableStateFlow<String> = MutableStateFlow(greenWallet.name)

    override val isAlwaysAsk: MutableStateFlow<Boolean> = MutableStateFlow(false)

    companion object {
        fun preview() = Bip39PassphraseViewModelPreview(
            previewWallet()
        )
    }
}