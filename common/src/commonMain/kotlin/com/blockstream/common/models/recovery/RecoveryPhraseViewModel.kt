package com.blockstream.common.models.recovery

import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.Credentials
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.events.Event
import com.rickclephas.kmm.viewmodel.MutableStateFlow
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.MutableStateFlow

abstract class RecoveryPhraseViewModelAbstract(val isLightning: Boolean, greenWallet: GreenWallet?) :
    GreenViewModel(greenWalletOrNull = greenWallet) {
    override fun screenName(): String = "RecoveryPhrase"

    @NativeCoroutinesState
    abstract val mnemonic: MutableStateFlow<String>

    @NativeCoroutinesState
    abstract val mnemonicWords: MutableStateFlow<List<String>>

    @NativeCoroutinesState
    abstract val passphrase: MutableStateFlow<String?>

    @NativeCoroutinesState
    abstract val showQR: MutableStateFlow<Boolean>
}

class RecoveryPhraseViewModel(
    isLightning: Boolean,
    credentialsFor2of3: Credentials?,
    greenWallet: GreenWallet?
) :
    RecoveryPhraseViewModelAbstract(isLightning = isLightning, greenWallet = greenWallet) {

    val credentials: Credentials by lazy {
        credentialsFor2of3 ?: (if (isLightning) {
            Credentials(mnemonic = session.deriveLightningMnemonic())
        } else {
            session.getCredentials()
        })
    }

    override val mnemonic = MutableStateFlow(viewModelScope, credentials.mnemonic)

    override val mnemonicWords = MutableStateFlow(viewModelScope, credentials.mnemonic.split(" "))

    override val passphrase = MutableStateFlow(viewModelScope, credentials.bip39Passphrase)

    override val showQR = MutableStateFlow(viewModelScope, false)

    class LocalEvents {
        object ShowQR : Event
    }

    init {
        bootstrap()
    }

    override fun handleEvent(event: Event) {
        super.handleEvent(event)

        if (event is LocalEvents.ShowQR) {
            showQR.value = true
        }
    }
}

class RecoveryPhraseViewModelPreview(isLightning: Boolean, greenWallet: GreenWallet?) :
    RecoveryPhraseViewModelAbstract(isLightning = isLightning, greenWallet = greenWallet) {
    override val mnemonic: MutableStateFlow<String> = MutableStateFlow(
        viewModelScope,
        "chalk verb patch cube sell west penalty fish park worry tribe tourist"
    )
    override val mnemonicWords: MutableStateFlow<List<String>> = MutableStateFlow(
        viewModelScope,
        "chalk verb patch cube sell west penalty fish park worry tribe tourist".split(" ")
    )

    override val passphrase: MutableStateFlow<String?> = MutableStateFlow(viewModelScope, null)

    override val showQR: MutableStateFlow<Boolean> = MutableStateFlow(viewModelScope, false)

    companion object {
        fun preview() = RecoveryPhraseViewModelPreview(
            isLightning = false,
            greenWallet = previewWallet(isHardware = false)
        )
    }
}