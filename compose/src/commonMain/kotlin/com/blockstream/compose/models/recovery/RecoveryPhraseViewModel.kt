package com.blockstream.compose.models.recovery

import androidx.lifecycle.viewModelScope
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_back_up_recovery_phrase
import blockstream_green.common.generated.resources.id_lightning
import com.blockstream.data.Urls
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.extensions.ifConnectedSuspend
import com.blockstream.compose.extensions.previewWallet
import com.blockstream.data.gdk.data.Credentials
import com.blockstream.compose.events.Event
import com.blockstream.compose.events.Events
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.navigation.NavData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

abstract class RecoveryPhraseViewModelAbstract(
    val isLightning: Boolean,
    greenWallet: GreenWallet?
) :
    GreenViewModel(greenWalletOrNull = greenWallet) {
    override fun screenName(): String = "RecoveryPhrase"
    abstract val mnemonic: StateFlow<String>
    abstract val mnemonicWords: StateFlow<List<String>>
    abstract val passphrase: StateFlow<String?>
    abstract val showQR: MutableStateFlow<Boolean>
}

class RecoveryPhraseViewModel(
    isLightningDerived: Boolean,
    providedCredentials: Credentials?,
    greenWallet: GreenWallet?
) :
    RecoveryPhraseViewModelAbstract(isLightning = isLightningDerived, greenWallet = greenWallet) {

    private val _mnemonic = MutableStateFlow("")
    override val mnemonic: StateFlow<String> = _mnemonic

    private val _mnemonicWords = MutableStateFlow(emptyList<String>())
    override val mnemonicWords: StateFlow<List<String>> = _mnemonicWords

    private val _passphrase = MutableStateFlow<String?>(null)
    override val passphrase: StateFlow<String?> = _passphrase

    override val showQR = MutableStateFlow(false)

    class LocalEvents {
        object ClickLearnMore : Events.OpenBrowser(Urls.HELP_BIP39_PASSPHRASE)
        object ShowQR : Event
    }

    init {
        viewModelScope.launch {
            _navData.value = NavData(
                title = getString(Res.string.id_back_up_recovery_phrase),
                subtitle = if (isLightningDerived) getString(Res.string.id_lightning) else null
            )

            (providedCredentials
                ?: session.ifConnectedSuspend { (if (isLightningDerived) Credentials(mnemonic = session.deriveLightningMnemonic()) else session.getCredentials()) })?.also { credentials ->
                _mnemonic.value = credentials.mnemonic ?: ""
                _mnemonicWords.value = credentials.mnemonic?.split(" ") ?: listOf()
                _passphrase.value = credentials.bip39Passphrase
            }
        }

        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        if (event is LocalEvents.ShowQR) {
            showQR.value = true
        }
    }
}

class RecoveryPhraseViewModelPreview(isLightning: Boolean, providedCredentials: Credentials? = null, greenWallet: GreenWallet?) :
    RecoveryPhraseViewModelAbstract(isLightning = isLightning, greenWallet = greenWallet) {

    override val mnemonic: MutableStateFlow<String> = MutableStateFlow(DummyMnemonic)
    override val mnemonicWords: MutableStateFlow<List<String>> = MutableStateFlow(DummyMnemonic.split(" "))
    override val passphrase: MutableStateFlow<String?> = MutableStateFlow(providedCredentials?.bip39Passphrase)
    override val showQR: MutableStateFlow<Boolean> = MutableStateFlow(false)

    companion object {
        val DummyMnemonic =
            "chalk verb patch cube sell west penalty fish park worry tribe tourist"

        fun preview() = RecoveryPhraseViewModelPreview(
            isLightning = false,
            greenWallet = previewWallet(isHardware = false)
        )

        fun previewBip39() = RecoveryPhraseViewModelPreview(
            isLightning = false,
            providedCredentials = Credentials(mnemonic = DummyMnemonic, bip39Passphrase = "Memorized_BIP39"),
            greenWallet = previewWallet(isHardware = false)
        )
    }
}
