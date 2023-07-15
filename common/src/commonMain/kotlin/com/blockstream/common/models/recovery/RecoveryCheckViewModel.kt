package com.blockstream.common.models.recovery

import com.blockstream.common.data.AppInfo
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.gdk.Wally
import com.blockstream.common.gdk.getBip39WordList
import com.blockstream.common.events.Event
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.Loggable
import com.blockstream.common.utils.getSecureRandom
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

    override val progress: Int = ((((setupArgs.page.takeIf { setupArgs.page in 1.. totalPages } ?: 1) + mnemonicTotalPages).toFloat() / totalPages) * 100).toInt()
    override val hightlightCorrectWord = get<AppInfo>().isDevelopment

    private val isLastPage = setupArgs.page == RecoveryPhraseChecks

    override val checkWordIndex: Int
    override val correctWordIndex: Int
    override val wordLeft: String
    override val wordRight: String
    override val words: List<String>

    private var correctWord: String

    class LocalEvents{
        class SelectWord(val word: String): Event
    }

    init {
        val isLastOrFirstPage = setupArgs.page == 1 || isLastPage
        val wordsPerPage = mnemonicWords.size / RecoveryPhraseChecks

        val offset = 0 + (wordsPerPage * (setupArgs.page - 1))

        val wordIndex = getSecureRandom().unsecureRandomInt(if(isLastOrFirstPage) 1 else 0 , wordsPerPage - if(isLastOrFirstPage) 1 else 0)
        correctWord = mnemonicWords[offset + wordIndex]

        val wordList = wally.getBip39WordList().shuffled()
        words = (wordList.subList(0, 3).toMutableList() + correctWord).shuffled()

        correctWordIndex = words.indexOf(correctWord)

        wordLeft = mnemonicWords[offset + wordIndex - 1]
        wordRight = mnemonicWords[offset + wordIndex + 1]
        checkWordIndex = offset + wordIndex + 1

        bootstrap()
    }

    override fun handleEvent(event: Event) {
        super.handleEvent(event)
        if(event is LocalEvents.SelectWord){
            if (correctWord == event.word) {

                (if(isLastPage){
                    if(setupArgs.greenWallet == null){
                        NavigateDestinations.SetPin(args = setupArgs.pageOne())
                    }else{
                        NavigateDestinations.AddAccount(args = setupArgs.pageOne())
                    }
                }else{
                    NavigateDestinations.RecoveryCheck(args = setupArgs.nextPage())
                }).also {
                    postSideEffect(SideEffects.NavigateTo(it))
                }

            } else {
                countly.recoveryPhraseCheckFailed(page = setupArgs.page)
                postSideEffect(SideEffects.Snackbar("id_wrong_choice_check_your"))
                postSideEffect(SideEffects.NavigateBack())
            }
        }
    }

    companion object: Loggable() {
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

    override val words: List<String> = listOf("chalk", "verb", "patch", "cube", "sell", "west")

    override val checkWordIndex: Int = 8
    override val correctWordIndex: Int = 2

    override val hightlightCorrectWord: Boolean = true
}