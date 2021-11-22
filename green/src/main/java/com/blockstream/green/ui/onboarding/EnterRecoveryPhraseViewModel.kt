package com.blockstream.green.ui.onboarding

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.blockstream.gdk.GreenWallet
import com.blockstream.green.ui.AppViewModel
import com.blockstream.green.R
import com.blockstream.green.ui.items.RecoveryPhraseWordListItem
import com.blockstream.green.views.RecoveryPhraseKeyboardView
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlin.properties.Delegates

class EnterRecoveryPhraseViewModel @AssistedInject constructor(
    private val appWallet: GreenWallet,
    @Assisted private val recoveryPhrase: String?,
    @Assisted private val isBip39: Boolean
) : AppViewModel() {

    val showPasteButton = MutableLiveData(true)
    val showHelpButton = MutableLiveData(false)
    val showInvalidMnemonicError = MutableLiveData(false)
    val isValid = MutableLiveData(false)
    val messageResource = MutableLiveData(0)
    val recoveryPhraseState =
        MutableLiveData(RecoveryPhraseKeyboardView.RecoveryPhraseState.fromString(recoveryPhrase))
    val recoveryWords = MutableLiveData<List<RecoveryPhraseWordListItem>>()
    var isEncryptionPasswordRequired = false

    var recoverySize: Int by Delegates.observable(recoveryPhraseState.value?.phrase?.size ?: 0) { property, oldValue, newValue ->
        recoveryPhraseState.value?.let {
            updateRecoveryPhrase(it)
        }
    }

    init {
        recoveryPhraseState.value?.let {
            updateRecoveryPhrase(it)
        }
    }

    fun updateRecoveryPhrase(state: RecoveryPhraseKeyboardView.RecoveryPhraseState) {
        recoveryPhraseState.value = state

        val list = mutableListOf<RecoveryPhraseWordListItem>()

        state.phrase.forEach { word ->
            list += RecoveryPhraseWordListItem(list.size + 1, word, state.activeIndex == list.size)
        }

        while (list.size < recoverySize) {
            list += RecoveryPhraseWordListItem(list.size + 1, "", list.isEmpty())
        }

        validate(state)

        recoveryWords.value = list
    }

    private fun validate(state: RecoveryPhraseKeyboardView.RecoveryPhraseState) {
        val recoveryPhrase = state.phrase
        val isEditMode = state.isEditMode


        var len = recoveryPhrase.size
        len -= if (isEditMode && len % 3 == 0) 1 else 0

        val valid = if (len > 11 && len % 3 == 0 && !isEditMode) {
            appWallet.isMnemonicValid(recoveryPhrase.joinToString(" "))
        } else {
            false
        }

        val msgRes: Int

        if (isBip39) {
            msgRes = if (len < 12) {
                R.string.id_enter_yournrecovery_phrase
            } else if (valid) {
                R.string.id_well_done_you_can_continue
            } else if (len == 12 || len == 24) {
                R.string.id_invalid_mnemonic_continue
            } else {
                R.string.id_enter_yournrecovery_phrase
            }

        } else {
            msgRes = if (len < 12) {
                R.string.id_enter_your_12_24_or_27_words
            } else if (valid) {
                R.string.id_well_done_you_can_continue
            } else if (len < 24) {
                R.string.id_enter_your_24_or_27_words
            } else if (valid && len == 27) {
                R.string.id_well_done_you_can_continue_with
            } else if (!valid && len == 24) {
                R.string.id_invalid_mnemonic_continue
            } else if (len < 27) {
                R.string.id_enter_your_27_words_recovery
            } else {
                0
            }
        }

        isEncryptionPasswordRequired = !isBip39 && len == 27

        val showHelp = !valid && (len >= 12 && len % 3 == 0)

        messageResource.value = msgRes

        isValid.value = valid

        showInvalidMnemonicError.value = showHelp && !isEditMode && len >= recoverySize
        showPasteButton.value = recoveryPhrase.size == 0
        showHelpButton.value = showHelp
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(recoveryPhrase: String?, isBip39: Boolean): EnterRecoveryPhraseViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            recoveryPhrase: String?,
            isBip39: Boolean
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(recoveryPhrase, isBip39) as T
            }
        }
    }
}
