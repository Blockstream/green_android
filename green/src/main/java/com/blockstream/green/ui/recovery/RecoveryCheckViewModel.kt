package com.blockstream.green.ui.recovery

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.blockstream.gdk.GdkBridge
import com.blockstream.green.data.Countly
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.ui.AppViewModel
import com.blockstream.green.utils.ConsumableEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import mu.KLogging
import java.security.SecureRandom
import kotlin.random.asKotlinRandom

class RecoveryCheckViewModel @AssistedInject constructor(
    gdkBridge: GdkBridge,
    countly: Countly,
    @Assisted val mnemonic: List<String>,
    @Assisted val page: Int
) : AppViewModel(countly) {
    val wordLeft = MutableLiveData<String>()
    val wordRight = MutableLiveData<String>()

    val words = MutableLiveData<List<String>>()
    val pointer = MutableLiveData(0)
    val numberOfWords = MutableLiveData(mnemonic.size)

    var correctWordIndex = -1
    private val correctWord: String
    var isLastPage = false

    val progress = MutableLiveData(0)
    val progressMax = MutableLiveData(0)
    init {

        val totalChecks = RecoveryPhraseChecks
        val wordsPerPage = mnemonic.size / totalChecks

        isLastPage = (page + 1) >= totalChecks
        val isLastOrFirstPage = page == 0 || isLastPage

        val offset = 0 + (wordsPerPage * page)

        // avoid picking the first or last word from the mnemonic as this will break the UI
        val wordIndex = SecureRandom().asKotlinRandom().nextInt(if(isLastOrFirstPage) 1 else 0 , wordsPerPage - if(isLastOrFirstPage) 1 else 0)
        correctWord = mnemonic[offset + wordIndex]

        val wordList = gdkBridge.getMnemonicWordList().shuffled()
        val randomChoices = (wordList.subList(0, 3).toMutableList() + correctWord).shuffled()

        correctWordIndex = randomChoices.indexOf(correctWord)

        wordLeft.value = mnemonic[offset + wordIndex - 1]
        wordRight.value = mnemonic[offset + wordIndex + 1]

        words.value = randomChoices
        pointer.value = (wordsPerPage * page) + wordIndex + 1

        val pagesFromWords = (mnemonic.size / RecoveryWordsViewModel.WORDS_PER_PAGE)
        progress.value = pagesFromWords + page + 1
        progressMax.value = pagesFromWords + RecoveryPhraseChecks
    }

    fun selectWord(selectedWord: String) {
        onEvent.postValue(
            ConsumableEvent(
                if (correctWord == selectedWord) {
                    NavigateEvent.Navigate
                } else {
                    countly.recoveryPhraseCheckFailed(page = page + 1)
                    NavigateEvent.NavigateBack()
                }
            )
        )
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            mnemonic: List<String>,
            page: Int
        ): RecoveryCheckViewModel
    }

    companion object: KLogging(){
        const val RecoveryPhraseChecks = 4

        fun provideFactory(
            assistedFactory: AssistedFactory,
            mnemonic: List<String>,
            page: Int
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(mnemonic, page) as T
            }
        }
    }
}