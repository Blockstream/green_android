package com.blockstream.green.ui.onboarding

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.blockstream.gdk.GreenWallet
import com.blockstream.green.R
import com.blockstream.green.ui.AppViewModel
import com.blockstream.green.ui.items.RecoveryPhraseWordListItem
import com.blockstream.green.views.RecoveryPhraseKeyboardView
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

class EnterRecoveryPhraseViewModel @AssistedInject constructor(
    private val greenWallet: GreenWallet,
    @Assisted private val recoveryPhrase: String?,
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

    private val _recoveryPhraseSize = MutableLiveData(12)
    val recoveryPhraseSize: LiveData<Int>
        get() = _recoveryPhraseSize

    init {
        recoveryPhraseState.value?.let {
            updateRecoveryPhrase(it)
        }
    }

    fun setRecoveryPhraseSize(length : Int){
        _recoveryPhraseSize.value = length
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

        while (list.size < _recoveryPhraseSize.value!!) {
            list += RecoveryPhraseWordListItem(list.size + 1, "", list.isEmpty())
        }

        _recoveryPhraseSize.postValue(when(list.size){
            in 0..12 -> 12
            in 12..24 -> 24
            else -> {
                27
            }
        })

        validate(state)

        recoveryWords.value = list
    }

    private fun validate(state: RecoveryPhraseKeyboardView.RecoveryPhraseState) {
        val recoveryPhrase = state.phrase
        val isEditMode = state.isEditMode


        var len = recoveryPhrase.size
        len -= if (isEditMode && len % 3 == 0) 1 else 0

        val valid = if (len > 11 && len % 3 == 0 && !isEditMode) {
            greenWallet.isMnemonicValid(recoveryPhrase.joinToString(" "))
        } else {
            false
        }

        val msgRes = if (len < 12) {
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

        isEncryptionPasswordRequired = len == 27

        val showHelp = !valid && (len >= 12 && len % 3 == 0)

        messageResource.value = msgRes

        isValid.value = valid

        showInvalidMnemonicError.value = showHelp && !isEditMode && len >= _recoveryPhraseSize.value!!
        showPasteButton.value = recoveryPhrase.size == 0
        showHelpButton.value = showHelp
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(recoveryPhrase: String?): EnterRecoveryPhraseViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            recoveryPhrase: String?
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(recoveryPhrase) as T
            }
        }
    }
}
