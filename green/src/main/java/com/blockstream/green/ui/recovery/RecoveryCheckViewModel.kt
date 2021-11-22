package com.blockstream.green.ui.recovery

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.blockstream.gdk.GreenWallet
import com.blockstream.green.ui.AppViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.security.SecureRandom
import kotlin.random.asKotlinRandom

class RecoveryCheckViewModel @AssistedInject constructor(
    greenWallet: GreenWallet,
    @Assisted val mnemonic: List<String>,
    @Assisted val page: Int,
    @Assisted val isDevelopmentFlavor: Boolean,
) : AppViewModel() {
    val wordLeft = MutableLiveData<String>()
    val wordRight = MutableLiveData<String>()

    val words = MutableLiveData<List<String>>()
    val pointer = MutableLiveData(0)
    val numberOfWords = MutableLiveData(mnemonic.size)

    var correctWordIndex = -1
    private val correctWord: String
    var isLastPage = false

    init {
        // 3 checks on 12-words recovery phrase, 4 on 24-words
        val totalChecks = if(mnemonic.size <= 12) 3 else 4
        val wordsPerPage = mnemonic.size / totalChecks

        isLastPage = (page + 1) >= totalChecks

        val from = 0 + (wordsPerPage * page)
        val to = from + wordsPerPage
        val pageWords = mnemonic.subList(from, to)

        val wordIndex = SecureRandom().asKotlinRandom().nextInt(1, wordsPerPage-1)
        correctWord = pageWords[wordIndex]

        val wordList = greenWallet.getMnemonicWordList().shuffled()
        val randomChoices = (wordList.subList(0, 3).toMutableList() + correctWord).shuffled()

        correctWordIndex = randomChoices.indexOf(correctWord)

        wordLeft.value = pageWords[wordIndex - 1]
        wordRight.value = pageWords[wordIndex + 1]

        words.value = randomChoices
        pointer.value = (wordsPerPage * page) + wordIndex + 1
    }

    fun selectWord(selectedWord: String): Boolean = correctWord == selectedWord

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            mnemonic: List<String>,
            page: Int,
            isDevelopmentFlavor: Boolean
        ): RecoveryCheckViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            mnemonic: List<String>,
            page: Int,
            isDevelopmentFlavor: Boolean
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(mnemonic, page, isDevelopmentFlavor) as T
            }
        }
    }
}