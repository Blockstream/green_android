package com.blockstream.green.ui.recovery

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.blockstream.green.ui.AppViewModel

class RecoveryWordsViewModel constructor(
    val mnemonic: List<String>,
    val page: Int
): AppViewModel() {

    val words = MutableLiveData<List<String>>()
    val counter = MutableLiveData(0)

    val progress = MutableLiveData(0)
    val progressMax = MutableLiveData(0)

    var isLastPage = false

    init {
        val totalPages = mnemonic.size / WORDS_PER_PAGE

        isLastPage = (page + 1) >= totalPages

        progress.value = page + 1
        progressMax.value = totalPages

        val from = 0 + (WORDS_PER_PAGE * page)
        words.value = mnemonic.subList(from , from + WORDS_PER_PAGE)
        counter.value = from
    }


    companion object {
        private const val WORDS_PER_PAGE = 6

        fun provideFactory(
            mnemonic: List<String>,
            page: Int
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return RecoveryWordsViewModel(mnemonic, page) as T
            }
        }
    }
}