package com.blockstream.green.ui.recovery

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.blockstream.gdk.GreenWallet
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.gdk.observable
import com.blockstream.green.ui.AppViewModel
import com.blockstream.green.utils.ConsumableEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import java.security.SecureRandom
import kotlin.random.asKotlinRandom

class RecoveryCheckViewModel @AssistedInject constructor(
    private val walletRepository: WalletRepository,
    greenWallet: GreenWallet,
    @Assisted val wallet: Wallet?,
    @Assisted val mnemonic: List<String>,
    @Assisted val page: Int,
    @Assisted val isDevelopmentFlavor: Boolean,
) : AppViewModel() {
    val navigate = MutableLiveData<ConsumableEvent<Boolean>>()

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

    fun selectWord(selectedWord: String) {

        if (correctWord == selectedWord) {
            if(isLastPage){

                if(wallet != null){
                    wallet.isRecoveryPhraseConfirmed = true

                    wallet.observable {
                        walletRepository.updateWalletSync(it)
                    }.subscribeBy(
                        onError = {
                            onError.postValue(ConsumableEvent(it))
                        },
                        onSuccess = {
                            navigate.postValue(ConsumableEvent(true))
                        }
                    ).addTo(disposables)
                }else{
                    navigate.postValue(ConsumableEvent(true))
                }

            }else{
                navigate.value = ConsumableEvent(true)
            }
        } else {
            navigate.value = ConsumableEvent(false)
        }

    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            wallet: Wallet?,
            mnemonic: List<String>,
            page: Int,
            isDevelopmentFlavor: Boolean
        ): RecoveryCheckViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            wallet: Wallet?,
            mnemonic: List<String>,
            page: Int,
            isDevelopmentFlavor: Boolean
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return assistedFactory.create(wallet, mnemonic, page, isDevelopmentFlavor) as T
            }
        }
    }
}