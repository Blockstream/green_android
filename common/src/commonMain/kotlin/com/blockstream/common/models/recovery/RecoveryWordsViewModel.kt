package com.blockstream.common.models.recovery

import com.blockstream.common.data.SetupArgs
import com.blockstream.common.events.Events
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.recovery.RecoveryCheckViewModel.Companion.RecoveryPhraseChecks
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.green.utils.Loggable
import com.blockstream.ui.events.Event
import com.blockstream.ui.navigation.NavData
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope

abstract class RecoveryWordsViewModelAbstract(val setupArgs: SetupArgs) :
    GreenViewModel(greenWalletOrNull = setupArgs.greenWallet) {
    override fun screenName(): String = "RecoveryWords"

    abstract val progress: Int
    abstract val totalPages: Int
    abstract val startIndex: Int
    abstract val words: List<String>
}

class RecoveryWordsViewModel(setupArgs: SetupArgs) : RecoveryWordsViewModelAbstract(setupArgs = setupArgs) {
    private val mnemonicWords = setupArgs.mnemonicAsWords
    override val totalPages = mnemonicWords.size / WORDS_PER_PAGE

    override val progress: Int =
        ((((setupArgs.page.takeIf { setupArgs.page in 1..totalPages } ?: 1)).toFloat() / (totalPages + RecoveryPhraseChecks)) * 100).toInt()

    override val startIndex: Int

    override val words: List<String>

    private val isLastPage
        get() = setupArgs.page == totalPages

    init {
        val from = 0 + (WORDS_PER_PAGE * ((setupArgs.page.takeIf { setupArgs.page in 1..totalPages } ?: 1) - 1))
        startIndex = from + 1

        words = mnemonicWords.subList(from, from + WORDS_PER_PAGE)

        viewModelScope.launch {
            _navData.value =
                NavData(title = setupArgs.accountType?.toString(), subtitle = setupArgs.accountType?.let { greenWalletOrNull?.name })
        }

        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)
        if (event is Events.Continue) {
            (if (isLastPage) {
                NavigateDestinations.RecoveryCheck(setupArgs = setupArgs.pageOne())
            } else {
                NavigateDestinations.RecoveryWords(setupArgs = setupArgs.nextPage())
            }).also {
                postSideEffect(SideEffects.NavigateTo(it))
            }
        }
    }

    companion object : Loggable() {
        const val WORDS_PER_PAGE = 6
    }
}

class RecoveryWordsViewModelPreview(setupArgs: SetupArgs) : RecoveryWordsViewModelAbstract(setupArgs = setupArgs) {
    override val progress: Int
        get() = 10
    override val totalPages: Int
        get() = 1
    override val startIndex: Int
        get() = 1

    override val words: List<String>
        get() = listOf("chalk", "verb", "patch", "cube", "sell", "west")

    companion object {
        fun preview() = RecoveryWordsViewModelPreview(setupArgs = SetupArgs(mnemonic = "neutral inherit learn"))
    }
}