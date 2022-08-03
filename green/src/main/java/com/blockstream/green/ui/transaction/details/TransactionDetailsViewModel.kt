package com.blockstream.green.ui.transaction.details

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.blockstream.gdk.data.Account
import com.blockstream.gdk.data.Transaction
import com.blockstream.gdk.params.TransactionParams
import com.blockstream.green.data.Countly
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.managers.SessionManager
import com.blockstream.green.ui.bottomsheets.ITransactionNote
import com.blockstream.green.ui.wallet.AbstractAccountWalletViewModel
import com.blockstream.green.utils.ConsumableEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class TransactionDetailsViewModel @AssistedInject constructor(
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    countly: Countly,
    @Assisted wallet: Wallet,
    @Assisted account: Account,
    @Assisted val initialTransaction: Transaction
) : AbstractAccountWalletViewModel(sessionManager, walletRepository, countly, wallet, account),
    ITransactionNote {

    val transactionLiveData = MutableLiveData<Transaction>()

    private val transactionNoteLiveData = MutableLiveData(initialTransaction.memo)
    val transactionNote get() = transactionNoteLiveData.value ?: ""

    init {
        // Update transaction data and create a copy with a stable memo so that we can properly animate
        combine(
            session.walletTransactionsFlow,
            session.accountTransactionsFlow(account),
            session.blockFlow(account.network)
        ) { walletTransactions, accountTransactions, _ ->
            // Be sure to find the correct tx not just by hash but also with the correct type (cross-account transactions)
            walletTransactions.find { it.txHash == initialTransaction.txHash && it.txType == initialTransaction.txType }
                ?: accountTransactions.find { it.txHash == initialTransaction.txHash }
        }.onEach {
            transactionLiveData.value = stabilizeTransaction(it ?: initialTransaction)
        }.launchIn(viewModelScope)
    }

    private fun stabilizeTransaction(tx: Transaction): Transaction {
        return tx.copy(memo = "STABLE_FOR_EQUALS").also {
            it.accountInjected = tx.accountInjected
        }
    }

    override fun saveNote(note: String) {
        if(session.setTransactionMemo(network, txHash = transactionLiveData.value!!.txHash, note)){
            transactionNoteLiveData.postValue(note)
            // update transaction
            session.getTransactions(account = account, isReset = false, isLoadMore = false)
            session.updateWalletTransactions(updateForAccounts = listOf(account))
        }else{
            onError.postValue(ConsumableEvent(Exception("id_error")))
        }
    }

    fun bumpFee() {
        doUserAction({
            val transactions = session.getTransactions(
                network,
                TransactionParams(
                    subaccount = initialTransaction.accountInjected?.pointer ?: 0,
                    confirmations = 0
                )
            )

            transactions
                .transactions
                .indexOfFirst { it.txHash == initialTransaction.txHash } // Find the index of the transaction
                .takeIf { it >= 0 }?.let { index ->
                    transactions.jsonElement?.jsonObject?.get("transactions")?.jsonArray?.getOrNull(index)
                }?.let {
                    Json.encodeToString(it)
                } ?: throw Exception("Couldn't find the transaction")
        }, onSuccess = {
            onEvent.postValue(ConsumableEvent(NavigateEvent.NavigateWithData(it)))
        })
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            wallet: Wallet,
            account: Account,
            transaction: Transaction
        ): TransactionDetailsViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            wallet: Wallet,
            account: Account,
            transaction: Transaction
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(wallet, account, transaction) as T
            }
        }
    }
}