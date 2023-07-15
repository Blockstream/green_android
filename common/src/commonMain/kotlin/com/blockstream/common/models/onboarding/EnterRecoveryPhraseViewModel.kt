package com.blockstream.common.models.onboarding

import com.blockstream.common.data.SetupArgs
import com.blockstream.common.gdk.Wally
import com.blockstream.common.gdk.getBip39WordList
import com.blockstream.common.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.data.Redact
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.rickclephas.kmm.viewmodel.MutableStateFlow
import com.rickclephas.kmm.viewmodel.coroutineScope
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import org.koin.core.component.inject

abstract class EnterRecoveryPhraseViewModelAbstract(val setupArgs: SetupArgs): GreenViewModel(setupArgs.greenWallet) {
    override fun screenName(): String = "OnBoardEnterRecovery"

    override fun segmentation(): HashMap<String, Any>? = setupArgs.let { countly.onBoardingSegmentation(setupArgs = it) }

    @NativeCoroutinesState
    abstract val recoveryPhrase: MutableStateFlow<List<String>>

    @NativeCoroutinesState
    abstract val activeWord: MutableStateFlow<Int>

    @NativeCoroutinesState
    abstract val isRecoveryPhraseValid: StateFlow<Boolean>

    @NativeCoroutinesState
    abstract val showInputButtons: StateFlow<Boolean>

    @NativeCoroutinesState
    abstract val showHelpButton: StateFlow<Boolean>

    @NativeCoroutinesState
    abstract val showTypeNextWordHint: StateFlow<Boolean>

    @NativeCoroutinesState
    abstract val showInvalidMnemonicError: StateFlow<Boolean>

    @NativeCoroutinesState
    abstract val recoveryPhraseSize: MutableStateFlow<Int>

    @NativeCoroutinesState
    abstract val hintMessage: MutableStateFlow<String?>

    abstract val bip39WordList: List<String>
}

class EnterRecoveryPhraseViewModel(setupArgs: SetupArgs): EnterRecoveryPhraseViewModelAbstract(setupArgs) {
    val wally: Wally by inject()

    override val recoveryPhrase: MutableStateFlow<List<String>> = MutableStateFlow(viewModelScope, listOf())
    override val activeWord: MutableStateFlow<Int> = MutableStateFlow(viewModelScope, -1)

    override val isRecoveryPhraseValid: MutableStateFlow<Boolean> = MutableStateFlow(viewModelScope, false)
    override val showInputButtons: MutableStateFlow<Boolean> = MutableStateFlow(viewModelScope, true)
    override val showHelpButton: MutableStateFlow<Boolean> = MutableStateFlow(viewModelScope, false)
    override val showTypeNextWordHint: MutableStateFlow<Boolean> = MutableStateFlow(viewModelScope, false)
    override val showInvalidMnemonicError: MutableStateFlow<Boolean> = MutableStateFlow(viewModelScope, false)
    override val recoveryPhraseSize: MutableStateFlow<Int> = MutableStateFlow(viewModelScope, 12)
    override val hintMessage: MutableStateFlow<String?> = MutableStateFlow(viewModelScope, null)

    override val bip39WordList: List<String> = wally.getBip39WordList()

    override val isLoginRequired: Boolean
        get() = setupArgs.isAddAccount()

    class LocalEvents{
        data class SetRecoveryPhrase(val recoveryPhrase: String): Event, Redact
        data class MnemonicEncryptionPassword(val password: String): Event, Redact
    }

    class LocalSideEffects{
        class RequestMnemonicPassword: SideEffect
    }

    init {
        bootstrap()

        combine(recoveryPhrase, activeWord) { _, _ ->
            checkRecoveryPhrase()
        }.launchIn(viewModelScope.coroutineScope)
    }

    override fun handleEvent(event: Event) {
        super.handleEvent(event)

        if(event is Events.Continue){
            if (recoveryPhrase.value.size == 27) {
                postSideEffect(LocalSideEffects.RequestMnemonicPassword())
            } else {
                proceed(setupArgs.copy(mnemonic = toMnemonic()))
            }
        } else if (event is LocalEvents.MnemonicEncryptionPassword){
            proceed(setupArgs.copy(mnemonic = toMnemonic(), password = event.password))

        } else if (event is LocalEvents.SetRecoveryPhrase){
            // Basic check to prevent huge inputs
            recoveryPhrase.value = event.recoveryPhrase
                .trim()
                .replace("\n", " ")
                .replace("\\s+", "")
                .split(" ").takeIf { it.size <= 32 }?.toMutableList() ?: mutableListOf()
            activeWord.value = -1
        }
    }

    private fun proceed(setupArgs: SetupArgs){
        setupArgs.let { args ->
            if(args.isAddAccount()){
                NavigateDestinations.AddAccount(args)
            }else{
                NavigateDestinations.SetPin(args)
            }
        }.also {
            postSideEffect(SideEffects.NavigateTo(it))
        }
    }

    private fun hasActiveWord(): Boolean{
        return activeWord.value >= 0 && activeWord.value < recoveryPhrase.value.size
    }

    private fun activeWord(): CharSequence? = if(hasActiveWord()) recoveryPhrase.value[activeWord.value] else null

    private fun checkRecoveryPhrase(){
        val isEditMode = activeWord.value != -1

        var len = recoveryPhrase.value.size
        len -= if (isEditMode && len % 3 == 0) 1 else 0

        val valid = if (len > 11 && len % 3 == 0 && !isEditMode) {
            wally.bip39MnemonicValidate(toMnemonic())
        } else {
            false
        }

        val hint = if (len < 12) {
            "id_enter_your_12_24_or_27_words"
        } else if (valid) {
            "id_well_done_you_can_continue"
        } else if (len < 24) {
            "id_enter_your_24_or_27_words"
        } else if (valid && len == 27) {
            "id_well_done_you_can_continue_with"
        } else if (!valid && len == 24) {
            "id_invalid_mnemonic_continue"
        } else if (len < 27) {
            "id_enter_your_27_words_recovery"
        } else {
            null
        }

        val showHelp = !valid && (len >= 12 && len % 3 == 0)

        hintMessage.value = hint

        isRecoveryPhraseValid.value = valid
        showInvalidMnemonicError.value = showHelp && !isEditMode && len >= recoveryPhraseSize.value
        showInputButtons.value = recoveryPhrase.value.isEmpty()
        showHelpButton.value = showHelp
        showTypeNextWordHint.value = recoveryPhrase.value.size in 1..11 && activeWord().isNullOrBlank()
    }

    private fun toMnemonic(): String {
        return recoveryPhrase.value.joinToString(" ")
    }
}

class EnterRecoveryPhraseViewModelPreview(setupArgs: SetupArgs): EnterRecoveryPhraseViewModelAbstract(setupArgs) {
    val wally: Wally by inject()

    override val recoveryPhrase: MutableStateFlow<List<String>> = MutableStateFlow(viewModelScope, emptyList())

    override val activeWord: MutableStateFlow<Int> = MutableStateFlow(viewModelScope, -1)

    override val isRecoveryPhraseValid: StateFlow<Boolean> = MutableStateFlow(viewModelScope, false)

    override val showInputButtons: StateFlow<Boolean> = MutableStateFlow(viewModelScope, true)

    override val showHelpButton: StateFlow<Boolean> = MutableStateFlow(viewModelScope, false)

    override val showTypeNextWordHint: StateFlow<Boolean> = MutableStateFlow(viewModelScope, false)

    override val showInvalidMnemonicError: StateFlow<Boolean> = MutableStateFlow(viewModelScope, false)
    override val recoveryPhraseSize: MutableStateFlow<Int> = MutableStateFlow(viewModelScope, 24)

    override val hintMessage: MutableStateFlow<String?> = MutableStateFlow(viewModelScope, null)

    override val bip39WordList: List<String> = wally.getBip39WordList()

    companion object{
        fun preview() = EnterRecoveryPhraseViewModelPreview(SetupArgs())
    }
}