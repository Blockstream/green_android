package com.blockstream.common.models.onboarding.phone

import com.arkivanov.essenty.statekeeper.StateKeeper
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import com.blockstream.common.data.NavAction
import com.blockstream.common.data.NavData
import com.blockstream.common.data.Redact
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.gdk.Wally
import com.blockstream.common.gdk.getBip39WordList
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.Loggable
import com.rickclephas.kmp.observableviewmodel.MutableStateFlow
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.serialization.Serializable
import org.koin.core.component.inject
import kotlin.math.ceil
import kotlin.math.max

abstract class EnterRecoveryPhraseViewModelAbstract(val setupArgs: SetupArgs) :
    GreenViewModel(setupArgs.greenWallet) {
    override fun screenName(): String = "OnBoardEnterRecovery"

    override fun segmentation(): HashMap<String, Any>? =
        setupArgs.let { countly.onBoardingSegmentation(setupArgs = it) }

    @NativeCoroutinesState
    abstract val recoveryPhrase: MutableStateFlow<List<String>>

    @NativeCoroutinesState
    abstract val rows: MutableStateFlow<Int>

    @NativeCoroutinesState
    abstract val activeWord: MutableStateFlow<Int>

    @NativeCoroutinesState
    abstract val matchedWords: MutableStateFlow<List<String>>

    @NativeCoroutinesState
    abstract val enabledKeys: MutableStateFlow<Set<String>>

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
    abstract val hintMessage: MutableStateFlow<String>

    abstract val bip39WordList: List<String>
}

class EnterRecoveryPhraseViewModel(setupArgs: SetupArgs, stateKeeper: StateKeeper = StateKeeperDispatcher()) :
    EnterRecoveryPhraseViewModelAbstract(setupArgs) {
    val wally: Wally by inject()

    override val recoveryPhrase: MutableStateFlow<List<String>> =
        MutableStateFlow(viewModelScope, listOf())
    override val rows: MutableStateFlow<Int> = MutableStateFlow(viewModelScope, 12)
    override val activeWord: MutableStateFlow<Int> = MutableStateFlow(viewModelScope, -1)

    override val matchedWords = MutableStateFlow(viewModelScope, listOf<String>())
    override val enabledKeys = MutableStateFlow(viewModelScope, setOf<String>())

    override val isRecoveryPhraseValid: MutableStateFlow<Boolean> =
        MutableStateFlow(viewModelScope, false)
    override val showInputButtons: MutableStateFlow<Boolean> =
        MutableStateFlow(viewModelScope, true)
    override val showHelpButton: MutableStateFlow<Boolean> = MutableStateFlow(viewModelScope, false)
    override val showTypeNextWordHint: MutableStateFlow<Boolean> =
        MutableStateFlow(viewModelScope, false)
    override val showInvalidMnemonicError: MutableStateFlow<Boolean> =
        MutableStateFlow(viewModelScope, false)
    override val recoveryPhraseSize: MutableStateFlow<Int> = MutableStateFlow(viewModelScope, 12)
    override val hintMessage: MutableStateFlow<String> = MutableStateFlow(viewModelScope, "")

    override val bip39WordList: List<String> by lazy { wally.getBip39WordList() }

    override val isLoginRequired: Boolean
        get() = setupArgs.isAddAccount()

    class LocalEvents {
        data class SetRecoveryPhrase(val recoveryPhrase: String) : Event, Redact
        data class SetActiveWord(val index: Int) : Event
        data class MnemonicEncryptionPassword(val password: String) : Event, Redact
        data class KeyAction(val key: String) : Event
    }

    class LocalSideEffects {
        object LaunchHelp : SideEffect
        class RequestMnemonicPassword : SideEffect
    }

    init {
        bootstrap()

        stateKeeper.register(STATE, State.serializer()) {
            State(mnemonic = toMnemonic())
        }

        stateKeeper.consume(STATE, State.serializer())?.mnemonic.takeIf { it.isNotBlank() }?.also {
            setRecoveryPhrase(it)
        }

        _navData.value = NavData(
            actions = listOf(NavAction(
                title = "id_help",
                icon = "question",
                onClick = {
                    postSideEffect(LocalSideEffects.LaunchHelp)
                }
            ))
        )

        combine(recoveryPhrase,recoveryPhraseSize, activeWord) { _, _, _ ->
            checkRecoveryPhrase()
        }.launchIn(viewModelScope.coroutineScope)
    }

    override fun handleEvent(event: Event) {
        super.handleEvent(event)

        if (event is Events.Continue) {
            if (recoveryPhrase.value.size == 27) {
                postSideEffect(LocalSideEffects.RequestMnemonicPassword())
            } else {
                proceed(setupArgs.copy(mnemonic = toMnemonic()))
            }
        } else if (event is LocalEvents.MnemonicEncryptionPassword) {
            proceed(setupArgs.copy(mnemonic = toMnemonic(), password = event.password))
        } else if (event is LocalEvents.SetRecoveryPhrase) {
            setRecoveryPhrase(event.recoveryPhrase)
        } else if (event is LocalEvents.KeyAction) {
            keyAction(event.key)
        } else if (event is LocalEvents.SetActiveWord) {
            activeWord.value = if (event.index <= recoveryPhrase.value.size) {
                event.index
            } else -1
        }
    }

    private fun setRecoveryPhrase(phrase: String){
        // Basic check to prevent huge inputs
        recoveryPhrase.value = phrase
            .trim()
            .replace("\n", " ")
            .replace("\\s+", "")
            .takeIf { it.isNotBlank() }
            ?.split(" ")?.takeIf { it.size <= 32 }?.toMutableList() ?: mutableListOf()
        activeWord.value = -1
    }

    private fun proceed(setupArgs: SetupArgs) {
        setupArgs.let { args ->
            if (args.isAddAccount()) {
                NavigateDestinations.AddAccount(args)
            } else {
                NavigateDestinations.SetPin(args)
            }
        }.also {
            postSideEffect(SideEffects.NavigateTo(it))
        }
    }

    private fun hasActiveWord(): Boolean {
        return activeWord.value >= 0 && activeWord.value < recoveryPhrase.value.size
    }

    private fun activeWord(): String? =
        if (hasActiveWord()) recoveryPhrase.value[activeWord.value] else null

    private fun checkRecoveryPhrase() {
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
            ""
        }

        val showHelp = !valid && (len >= 12 && len % 3 == 0)

        hintMessage.value = hint

        isRecoveryPhraseValid.value = valid
        showInvalidMnemonicError.value = showHelp && !isEditMode && len >= recoveryPhraseSize.value
        showInputButtons.value = recoveryPhrase.value.isEmpty()
        showHelpButton.value = showHelp
        showTypeNextWordHint.value =
            recoveryPhrase.value.size in 1..11 && activeWord().isNullOrEmpty()


        val active = activeWord() ?: ""

        val matched = mutableListOf<String>()
        val enabled = mutableSetOf<String>()

        bip39WordList.forEach { word ->
            if(active.isEmpty() || word.startsWith(active)){
                if(active.isNotBlank() && matched.size < 4){
                    matched.add(word)
                }

                if(active.length < word.length){
                    enabled.add(word[active.length].toString())
                }
            }
        }

        matchedWords.value = if(activeWord.value != -1) matched else listOf()

        // Disable keys for words greater than 27
        enabledKeys.value = if(recoveryPhrase.value.count() >= 27 && activeWord.value != -1) setOf() else enabled

        rows.value = (ceil(max(recoveryPhrase.value.size, recoveryPhraseSize.value) / 3f).toInt())
    }

    private fun removeWord() {
        if(activeWord.value < recoveryPhrase.value.size) {
            if (activeWord.value != -1) {
                recoveryPhrase.value =
                    recoveryPhrase.value.toMutableList().also { it.removeAt(activeWord.value) }
            } else if (recoveryPhrase.value.isNotEmpty()) {
                recoveryPhrase.value =
                    recoveryPhrase.value.toMutableList().also { it.removeLastOrNull() }.toList()
            }
        }
        activeWord.value = -1
    }

    private fun append(word: String) {
        if (activeWord.value == -1) {
            recoveryPhrase.value = recoveryPhrase.value.toMutableList().also { it.add(word) }
        } else {
            recoveryPhrase.value =
                recoveryPhrase.value.toMutableList().also { it[activeWord.value] = word }
        }
        activeWord.value = -1
    }

    private fun keyAction(key: String) {
        if (key.length > 1) {
           append(key)
        } else if (key == " ") {
            if (activeWord.value == -1) {
                activeWord.value = recoveryPhrase.value.size - 1
            } else if (recoveryPhrase.value.isNotEmpty()) {
                val word = recoveryPhrase.value.getOrNull(activeWord.value)
                if (word.isNullOrEmpty()) {
                    removeWord()
                } else {
                    recoveryPhrase.value = recoveryPhrase.value.toMutableList().also {
                        it[activeWord.value] = word.dropLast(1)
                    }
                }

                // Case where you are on the last empty word, its better on that case to completely
                // remove the word, that way we can immediately show the paste button
                if (recoveryPhrase.value.size == 1 && recoveryPhrase.value[0].isBlank()) {
                    removeWord()
                }
            }

        } else {
            if (activeWord.value == -1) {
                recoveryPhrase.value = recoveryPhrase.value.toMutableList().also { it.add(key) }
                activeWord.value = recoveryPhrase.value.size - 1
            } else {
                recoveryPhrase.value = recoveryPhrase.value.toMutableList().also {
                    val newWord = ((it.getOrNull(activeWord.value) ?: "") + key)
                    if (activeWord.value < it.size) {
                        it[activeWord.value] = newWord
                    } else {
                        it.add(newWord)
                    }
                }

                val activeWord = activeWord()
                if(activeWord != null && matchedWords.value.firstOrNull() == activeWord){
                    append(activeWord)
                }
            }
        }
    }

    private fun toMnemonic(): String {
        return recoveryPhrase.value.joinToString(" ")
    }

    @Serializable
    private class State(
        val mnemonic: String
    )

    companion object: Loggable() {
        const val STATE = "STATE"
    }
}

class EnterRecoveryPhraseViewModelPreview(setupArgs: SetupArgs) :
    EnterRecoveryPhraseViewModelAbstract(setupArgs) {
    override val recoveryPhrase: MutableStateFlow<List<String>> =
        MutableStateFlow(viewModelScope, emptyList())

    override val rows: MutableStateFlow<Int> = MutableStateFlow(viewModelScope, 4)

    override val activeWord: MutableStateFlow<Int> = MutableStateFlow(viewModelScope, -1)

    override val matchedWords: MutableStateFlow<List<String>> = MutableStateFlow(viewModelScope, mutableListOf("about"))

    override val enabledKeys: MutableStateFlow<Set<String>> = MutableStateFlow(viewModelScope, setOf())

    override val isRecoveryPhraseValid: StateFlow<Boolean> = MutableStateFlow(viewModelScope, true)

    override val showInputButtons: StateFlow<Boolean> = MutableStateFlow(viewModelScope, true)

    override val showHelpButton: StateFlow<Boolean> = MutableStateFlow(viewModelScope, false)

    override val showTypeNextWordHint: StateFlow<Boolean> = MutableStateFlow(viewModelScope, false)

    override val showInvalidMnemonicError: StateFlow<Boolean> =
        MutableStateFlow(viewModelScope, false)
    override val recoveryPhraseSize: MutableStateFlow<Int> = MutableStateFlow(viewModelScope, 24)

    override val hintMessage: MutableStateFlow<String> = MutableStateFlow(viewModelScope, "")

    override val bip39WordList: List<String> = listOf()

    companion object {
        fun preview() = EnterRecoveryPhraseViewModelPreview(SetupArgs())
    }
}