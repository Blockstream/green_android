package com.blockstream.compose.models.recovery

import androidx.lifecycle.viewModelScope
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_wrong_choice_check_your
import com.blockstream.compose.events.Event
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.navigation.NavData
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.compose.utils.StringHolder
import com.blockstream.data.config.AppInfo
import com.blockstream.data.data.SetupArgs
import com.blockstream.data.gdk.Wally
import com.blockstream.data.gdk.getBip39WordList
import com.blockstream.data.utils.getSecureRandom
import com.blockstream.utils.Loggable
import kotlinx.coroutines.launch
import org.koin.core.component.get
import org.koin.core.component.inject

abstract class RecoveryCheckViewModelAbstract(val setupArgs: SetupArgs) :
    GreenViewModel(greenWalletOrNull = setupArgs.greenWallet) {
    override fun screenName(): String = "RecoveryCheck"

    abstract val progress: Int

    abstract val wordLeft: String
    abstract val wordRight: String
    abstract val words: List<String>

    abstract val hightlightCorrectWord: Boolean
    abstract val checkWordIndex: Int
    abstract val correctWordIndex: Int
}

class RecoveryCheckViewModel(setupArgs: SetupArgs) : RecoveryCheckViewModelAbstract(setupArgs = setupArgs) {
    val wally: Wally by inject()

    private val mnemonicWords = setupArgs.mnemonicAsWords

    val mnemonicTotalPages: Int = (mnemonicWords.size / WORDS_PER_PAGE)
    private val totalPages: Int = mnemonicTotalPages + RecoveryPhraseChecks

    override val progress: Int =
        ((((setupArgs.page.takeIf { setupArgs.page in 1..totalPages } ?: 1) + mnemonicTotalPages).toFloat() / totalPages) * 100).toInt()
    override val hightlightCorrectWord = get<AppInfo>().isDevelopment

    private val isLastPage = setupArgs.page == RecoveryPhraseChecks

    override val checkWordIndex: Int
    override val correctWordIndex: Int
    override val wordLeft: String
    override val wordRight: String
    override val words: List<String>

    private var correctWord: String

    class LocalEvents {
        class SelectWord(val word: String) : Event
    }

    init {
        val isLastOrFirstPage = setupArgs.page == 1 || isLastPage
        val wordsPerPage = mnemonicWords.size / RecoveryPhraseChecks

        val offset = 0 + (wordsPerPage * (setupArgs.page - 1))

        val wordIndex = getSecureRandom().unsecureRandomInt(if (isLastOrFirstPage) 1 else 0, wordsPerPage - if (isLastOrFirstPage) 1 else 0)
        correctWord = mnemonicWords[offset + wordIndex]

        val wordList = wally.getBip39WordList().shuffled()
        words = (wordList.subList(0, 3).toMutableList() + correctWord).shuffled()

        correctWordIndex = words.indexOf(correctWord)

        wordLeft = mnemonicWords[offset + wordIndex - 1]
        wordRight = mnemonicWords[offset + wordIndex + 1]
        checkWordIndex = offset + wordIndex + 1

        viewModelScope.launch {
            _navData.value =
                NavData(title = setupArgs.accountType?.toString(), subtitle = setupArgs.accountType?.let { greenWalletOrNull?.name })
        }

        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)
        if (event is LocalEvents.SelectWord) {
            if (correctWord == event.word) {
                if (isLastPage) {
                    if (setupArgs.greenWallet == null) {
                        postSideEffect(SideEffects.NavigateTo(NavigateDestinations.SetPin(setupArgs = setupArgs.pageOne())))
                    } else if (setupArgs.greenWallet?.isRecoveryConfirmed == false) {
                        recoveryConfirmed()
                    } else {
                        postSideEffect(
                            SideEffects.NavigateTo(
                                NavigateDestinations.ReviewAddAccount(
                                    setupArgs = setupArgs.pageOne()
                                )
                            )
                        )
                    }
                } else {
                    postSideEffect(
                        SideEffects.NavigateTo(
                            NavigateDestinations.RecoveryCheck(
                                setupArgs = setupArgs.nextPage()
                            )
                        )
                    )
                }
            } else {
                countly.recoveryPhraseCheckFailed(page = setupArgs.page)
                postSideEffect(SideEffects.Snackbar(StringHolder.create(Res.string.id_wrong_choice_check_your)))
                postSideEffect(SideEffects.NavigateBack())
            }
        }
    }

    private fun recoveryConfirmed() {
        doAsync({
            greenWallet.isRecoveryConfirmed = true
            database.updateWallet(greenWallet)
        }, onSuccess = {
            postSideEffect(SideEffects.NavigateToRoot())
        })
    }

    companion object : Loggable() {
        const val RecoveryPhraseChecks = 4
        const val WORDS_PER_PAGE = 6
    }
}

class RecoveryCheckViewModelPreview(setupArgs: SetupArgs) : RecoveryCheckViewModelAbstract(setupArgs) {
    companion object {
        fun preview() = RecoveryCheckViewModelPreview(SetupArgs(mnemonic = "neutral inherit learn"))
    }

    override val progress: Int = 60

    override val wordLeft: String = "about"

    override val wordRight: String = "rib"

    override val words: List<String> = listOf("chalk", "verb", "patch", "cube")

    override val checkWordIndex: Int = 8
    override val correctWordIndex: Int = 2

    override val hightlightCorrectWord: Boolean = true
}