package com.blockstream.green.ui.recovery

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.blockstream.green.data.Countly
import com.blockstream.green.ui.AppViewModel
import com.blockstream.green.ui.recovery.RecoveryCheckViewModel.Companion.RecoveryPhraseChecks
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

class RecoveryWordsViewModel @AssistedInject constructor(
    countly: Countly,
    @Assisted val mnemonic: List<String>,
    @Assisted val page: Int
): AppViewModel(countly) {

    val words = MutableLiveData<List<String>>()
    val counter = MutableLiveData(0)

    val progress = MutableLiveData(0)
    val progressMax = MutableLiveData(0)

    var isLastPage = false

    init {
        val totalPages = mnemonic.size / WORDS_PER_PAGE

        isLastPage = (page + 1) >= totalPages

        progress.value = page + 1
        progressMax.value = totalPages + RecoveryPhraseChecks

        val from = 0 + (WORDS_PER_PAGE * page)
        words.value = mnemonic.subList(from , from + WORDS_PER_PAGE)
        counter.value = from
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            mnemonic: List<String>,
            page: Int
        ): RecoveryWordsViewModel
    }

    companion object {
        const val WORDS_PER_PAGE = 6

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