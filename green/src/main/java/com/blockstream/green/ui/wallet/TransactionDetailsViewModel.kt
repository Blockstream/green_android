package com.blockstream.green.ui.wallet

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.blockstream.gdk.data.Transaction
import com.blockstream.gdk.params.TransactionParams
import com.blockstream.green.data.Countly
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.gdk.observable
import com.blockstream.green.utils.ConsumableEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class TransactionDetailsViewModel @AssistedInject constructor(
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    countly: Countly,
    @Assisted wallet: Wallet,
    @Assisted  val initialTransaction: Transaction
) : AbstractWalletViewModel(sessionManager, walletRepository, countly, wallet) {

    val transaction = MutableLiveData<Transaction>()
    val editableNote = MutableLiveData(initialTransaction.memo)
    val originalNote = MutableLiveData(initialTransaction.memo)

    private var txIndex = -1

    init {

        // Update transaction data and create a copy with a stable memo so that we can properly animate
        session
            .getTransationsObservable()
            .map {
                txIndex = it.indexOfFirst { it.txHash == initialTransaction.txHash }
                (if(txIndex == -1) initialTransaction else it[txIndex]).copy(memo = "STABLE_FOR_EQUALS")
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onError = {

                },
                onNext = {
                    transaction.postValue(it)
                }
            ).addTo(disposables)
    }

    fun saveNote() {
        val note = editableNote.value ?: ""
        if(session.setTransactionMemo(txHash = transaction.value!!.txHash, note)){
            originalNote.postValue(note)
            // set the same value again so that the observer can turn the save button off
            editableNote.postValue(note)

            // update
            session.updateTransactionsAndBalance(isReset = false, isLoadMore = false)
        }else{
            onError.postValue(ConsumableEvent(Exception("id_error")))
        }
    }

    fun bumpFee() {
        session.observable {

            val transactions = it.getTransactions(
                TransactionParams(
                    subaccount = wallet.activeAccount,
                    offset = 0,
                    limit = txIndex + 1,
                    confirmations = 0
                )
            )

            val transaction = transactions.jsonElement?.jsonObject?.get("transactions")?.jsonArray?.get(txIndex)

            // check if the transaction found is correct
            if(transaction?.jsonObject?.get("txhash")?.jsonPrimitive?.content != initialTransaction.txHash){
                throw Exception("Couldn't find the transaction")
            }

            Json.encodeToString(transaction)

        }.doOnSubscribe {
            onProgress.postValue(true)
        }.doOnTerminate {
            onProgress.postValue(false)
        }.subscribeBy(
            onSuccess = {
                onEvent.postValue(ConsumableEvent(NavigateEvent.NavigateWithData(it)))
            },
            onError = {
                it.printStackTrace()
                onError.postValue(ConsumableEvent(it))
            }
        )
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            wallet: Wallet,
            transaction: Transaction
        ): TransactionDetailsViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            wallet: Wallet,
            transaction: Transaction
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(wallet, transaction) as T
            }
        }
    }
}