package com.blockstream.common.models.onboarding.phone

import androidx.lifecycle.viewModelScope
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_enter_your_12_24_or_27_words
import blockstream_green.common.generated.resources.id_enter_your_24_or_27_words
import blockstream_green.common.generated.resources.id_enter_your_27_words_recovery
import blockstream_green.common.generated.resources.id_help
import blockstream_green.common.generated.resources.id_invalid_mnemonic_continue
import blockstream_green.common.generated.resources.id_recovery_phrase_check
import blockstream_green.common.generated.resources.id_restoring_your_wallet
import blockstream_green.common.generated.resources.id_well_done_you_can_continue
import blockstream_green.common.generated.resources.id_well_done_you_can_continue_with
import blockstream_green.common.generated.resources.question
import com.arkivanov.essenty.statekeeper.StateKeeper
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import com.blockstream.common.crypto.BiometricsException
import com.blockstream.common.crypto.PlatformCipher
import com.blockstream.common.data.Redact
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.gdk.Wally
import com.blockstream.common.gdk.getBip39WordList
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.usecases.CheckRecoveryPhraseUseCase
import com.blockstream.common.utils.randomChars
import com.blockstream.domain.wallet.RestoreWalletUseCase
import com.blockstream.green.utils.Loggable
import com.blockstream.ui.events.Event
import com.blockstream.ui.navigation.NavAction
import com.blockstream.ui.navigation.NavData
import com.blockstream.ui.sideeffects.SideEffect
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.getString
import org.koin.core.component.inject
import kotlin.math.ceil
import kotlin.math.max

abstract class EnterRecoveryPhraseViewModelAbstract(val setupArgs: SetupArgs) :
    GreenViewModel(setupArgs.greenWallet) {
    override fun screenName(): String = "OnBoardEnterRecovery"

    override fun segmentation(): HashMap<String, Any>? =
        setupArgs.let { countly.onBoardingSegmentation(setupArgs = it) }
    abstract val recoveryPhrase: MutableStateFlow<List<String>>
    abstract val rows: MutableStateFlow<Int>
    abstract val activeWord: MutableStateFlow<Int>
    abstract val matchedWords: MutableStateFlow<List<String>>
    abstract val enabledKeys: MutableStateFlow<Set<String>>
    abstract val isRecoveryPhraseValid: StateFlow<Boolean>
    abstract val showInputButtons: StateFlow<Boolean>
    abstract val showHelpButton: StateFlow<Boolean>
    abstract val showTypeNextWordHint: StateFlow<Boolean>
    abstract val showInvalidMnemonicError: StateFlow<Boolean>
    abstract val recoveryPhraseSize: MutableStateFlow<Int>
    abstract val hintMessage: MutableStateFlow<String>

    abstract val bip39WordList: List<String>
}

class EnterRecoveryPhraseViewModel(setupArgs: SetupArgs, stateKeeper: StateKeeper = StateKeeperDispatcher()) :
    EnterRecoveryPhraseViewModelAbstract(setupArgs) {
    val wally: Wally by inject()

    private val restoreWalletUseCase: RestoreWalletUseCase by inject()
    private val checkRecoveryPhraseUseCase: CheckRecoveryPhraseUseCase by inject()
    override val recoveryPhrase: MutableStateFlow<List<String>> =
        MutableStateFlow(listOf())
    override val rows: MutableStateFlow<Int> = MutableStateFlow(12)
    override val activeWord: MutableStateFlow<Int> = MutableStateFlow(-1)
    override val matchedWords = MutableStateFlow(listOf<String>())
    override val enabledKeys = MutableStateFlow(setOf<String>())
    override val isRecoveryPhraseValid: MutableStateFlow<Boolean> =
        MutableStateFlow(false)
    override val showInputButtons: MutableStateFlow<Boolean> =
        MutableStateFlow(true)
    override val showHelpButton: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val showTypeNextWordHint: MutableStateFlow<Boolean> =
        MutableStateFlow(false)
    override val showInvalidMnemonicError: MutableStateFlow<Boolean> =
        MutableStateFlow(false)
    override val recoveryPhraseSize: MutableStateFlow<Int> = MutableStateFlow(12)
    override val hintMessage: MutableStateFlow<String> = MutableStateFlow("")

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
        class RequestMnemonicPassword : SideEffect
    }

    init {
        bootstrap()

        onProgress.onEach {
            _navData.value = _navData.value.copy(isVisible = !it)
        }.launchIn(this)

        stateKeeper.register(STATE, State.serializer()) {
            State(mnemonic = toMnemonic())
        }

        stateKeeper.consume(STATE, State.serializer())?.mnemonic.takeIf { it.isNotBlank() }?.also {
            setRecoveryPhrase(it)
        }

        viewModelScope.launch {
            _navData.value = NavData(
                title = setupArgs.accountType?.toString(),
                subtitle = greenWalletOrNull?.name,
                actions = listOf(
                    NavAction(
                        title = getString(Res.string.id_help),
                        icon = Res.drawable.question,
                        onClick = {
                            postSideEffect(SideEffects.NavigateTo(NavigateDestinations.RecoveryHelp))
                        }
                    ))
            )
        }

        combine(recoveryPhrase, recoveryPhraseSize, activeWord) { _, _, _ ->
            checkRecoveryPhrase()
        }.launchIn(viewModelScope)
    }

    override suspend fun handleEvent(event: Event) {
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

    private fun setRecoveryPhrase(phrase: String) {
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
        when {
            setupArgs.isAddAccount() -> postSideEffect(SideEffects.NavigateTo(NavigateDestinations.ReviewAddAccount(setupArgs)))
            !greenKeystore.canUseBiometrics() -> postSideEffect(SideEffects.NavigateTo(NavigateDestinations.SetPin(setupArgs)))
            else -> {
                restoreWallet(setupArgs)
            }
        }
    }

    private fun hasActiveWord(): Boolean {
        return activeWord.value >= 0 && activeWord.value < recoveryPhrase.value.size
    }

    private fun activeWord(): String? =
        if (hasActiveWord()) recoveryPhrase.value[activeWord.value] else null

    private suspend fun checkRecoveryPhrase() {
        val isEditMode = activeWord.value != -1

        var len = recoveryPhrase.value.size
        len -= if (isEditMode && len % 3 == 0) 1 else 0

        val valid = if (len > 11 && len % 3 == 0 && !isEditMode) {
            wally.bip39MnemonicValidate(toMnemonic())
        } else {
            false
        }

        val hint = if (len < 12) {
            Res.string.id_enter_your_12_24_or_27_words
        } else if (valid) {
            Res.string.id_well_done_you_can_continue
        } else if (len < 24) {
            Res.string.id_enter_your_24_or_27_words
        } else if (valid && len == 27) {
            Res.string.id_well_done_you_can_continue_with
        } else if (!valid && len == 24) {
            Res.string.id_invalid_mnemonic_continue
        } else if (len < 27) {
            Res.string.id_enter_your_27_words_recovery
        } else {
            null
        }

        val showHelp = !valid && (len >= 12 && len % 3 == 0)

        hintMessage.value = hint?.let { getString(it) } ?: ""

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
            if (active.isEmpty() || word.startsWith(active)) {
                if (active.isNotBlank() && matched.size < 4) {
                    matched.add(word)
                }

                if (active.length < word.length) {
                    enabled.add(word[active.length].toString())
                }
            }
        }

        matchedWords.value = if (activeWord.value != -1) matched else listOf()

        // Disable keys for words greater than 27
        enabledKeys.value = if (recoveryPhrase.value.count() >= 27 && activeWord.value != -1) setOf() else enabled

        rows.value = (ceil(max(recoveryPhrase.value.size, recoveryPhraseSize.value) / 3f).toInt())

        if (recoveryPhrase.value.size > recoveryPhraseSize.value) {
            if (recoveryPhrase.value.size > 24) {
                recoveryPhraseSize.value = 27
            } else {
                recoveryPhraseSize.value = 24
            }
        }
    }

    private fun removeWord() {
        if (activeWord.value < recoveryPhrase.value.size) {
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
                if (activeWord != null && matchedWords.value.let { it.size == 1 && it.firstOrNull() == activeWord }) {
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

    private fun restoreWallet(setupArgs: SetupArgs) {

        if (!greenKeystore.canUseBiometrics()) {
            postSideEffect(SideEffects.NavigateTo(NavigateDestinations.SetPin(setupArgs = setupArgs)))
            return
        }

        doAsync({
            val biometricsCipherProvider = viewModelScope.async(
                start = CoroutineStart.LAZY
            ) {
                CompletableDeferred<PlatformCipher>().let {
                    biometricsPlatformCipher = it
                    postSideEffect(SideEffects.RequestBiometricsCipher)
                    it.await()
                }
            }

            onProgressDescription.value = getString(Res.string.id_recovery_phrase_check)

            checkRecoveryPhraseUseCase.invoke(
                session = session,
                isTestnet = setupArgs.isTestnet == true,
                mnemonic = setupArgs.mnemonic,
                password = setupArgs.password,
                greenWallet = setupArgs.greenWallet
            )

            onProgressDescription.value = getString(Res.string.id_restoring_your_wallet)

            // If it's recovery wallet restore, use pin instead of biometrics
            if (setupArgs.greenWallet != null) {
                return@doAsync null
            }

            try {
                val cipher = if (greenKeystore.canUseBiometrics()) {
                    biometricsCipherProvider.await()
                } else return@doAsync null

                val pin = randomChars(15)

                restoreWalletUseCase.invoke(
                    session = session, setupArgs = setupArgs, pin = pin, greenWallet = greenWalletOrNull, cipher = cipher
                )

            } catch (e: Exception) {
                if (e.message == "id_action_canceled" || e is BiometricsException) {
                    null
                } else {
                    throw e
                }
            }
        }, onSuccess = {
            if (it != null) {
                postSideEffect(SideEffects.NavigateTo(NavigateDestinations.WalletOverview(it)))
            } else {
                postSideEffect(SideEffects.NavigateTo(NavigateDestinations.SetPin(setupArgs = setupArgs)))
            }
        })
    }

    companion object : Loggable() {
        const val STATE = "STATE"
    }
}

class EnterRecoveryPhraseViewModelPreview(setupArgs: SetupArgs) :
    EnterRecoveryPhraseViewModelAbstract(setupArgs) {
    override val recoveryPhrase: MutableStateFlow<List<String>> =
        MutableStateFlow(emptyList())

    override val rows: MutableStateFlow<Int> = MutableStateFlow(4)

    override val activeWord: MutableStateFlow<Int> = MutableStateFlow(-1)

    override val matchedWords: MutableStateFlow<List<String>> = MutableStateFlow(mutableListOf("about"))

    override val enabledKeys: MutableStateFlow<Set<String>> = MutableStateFlow(setOf())

    override val isRecoveryPhraseValid: StateFlow<Boolean> = MutableStateFlow(true)

    override val showInputButtons: StateFlow<Boolean> = MutableStateFlow(true)

    override val showHelpButton: StateFlow<Boolean> = MutableStateFlow(false)

    override val showTypeNextWordHint: StateFlow<Boolean> = MutableStateFlow(false)

    override val showInvalidMnemonicError: StateFlow<Boolean> =
        MutableStateFlow(false)
    override val recoveryPhraseSize: MutableStateFlow<Int> = MutableStateFlow(24)

    override val hintMessage: MutableStateFlow<String> = MutableStateFlow("")

    override val bip39WordList: List<String> = listOf()

    companion object {
        fun preview() = EnterRecoveryPhraseViewModelPreview(SetupArgs())
    }
}
