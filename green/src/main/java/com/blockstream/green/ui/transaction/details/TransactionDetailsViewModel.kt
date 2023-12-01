package com.blockstream.green.ui.transaction.details

import androidx.lifecycle.MutableLiveData
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.Transaction
import com.blockstream.common.gdk.params.TransactionParams
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.ConsumableEvent
import com.blockstream.common.views.TransactionDetailsLook
import com.blockstream.green.ui.bottomsheets.INote
import com.blockstream.green.ui.wallet.AbstractAccountWalletViewModel
import com.rickclephas.kmm.viewmodel.coroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

@KoinViewModel
class TransactionDetailsViewModel constructor(
    @InjectedParam wallet: GreenWallet,
    @InjectedParam account: Account,
    @InjectedParam val initialTransaction: Transaction
) : AbstractAccountWalletViewModel(wallet, account),
    INote {

    val transactionLiveData = MutableLiveData<Pair<Transaction, TransactionDetailsLook>>()

    private val transactionNoteLiveData = MutableLiveData(initialTransaction.memo)
    val transactionNote get() = transactionNoteLiveData.value ?: ""

    init {
        if(session.isConnected) {
            // Update transaction data and create a copy with a stable memo so that we can properly animate
            combine(
                session.walletTransactions,
                session.accountTransactions(account),
                session.block(account.network)
            ) { walletTransactions, accountTransactions, _ ->
                // Be sure to find the correct tx not just by hash but also with the correct type (cross-account transactions)
                walletTransactions.find { it.txHash == initialTransaction.txHash && it.txType == initialTransaction.txType }
                    ?: accountTransactions.find { it.txHash == initialTransaction.txHash }
            }.onEach {
                transactionLiveData.value =
                    stabilizeTransaction(it ?: initialTransaction) to TransactionDetailsLook.create(
                        session,
                        it ?: initialTransaction
                    )
            }.launchIn(viewModelScope.coroutineScope)
        }
    }

    private fun stabilizeTransaction(tx: Transaction): Transaction {
        return tx.copy(memo = "STABLE_FOR_EQUALS").also {
            it.accountInjected = tx.accountInjected
        }
    }

    override fun saveNote(note: String) {
        try {
            session.setTransactionMemo(
                network,
                txHash = transactionLiveData.value!!.first.txHash,
                note
            )

            transactionNoteLiveData.postValue(note)
            // update transaction
            session.getTransactions(account = accountValue, isReset = false, isLoadMore = false)
            session.updateWalletTransactions(updateForAccounts = listOf(accountValue))

        }catch (e: Exception){
            onError.postValue(ConsumableEvent(Exception("id_error")))
        }
    }

    fun bumpFee() {
        doUserAction({
            val transactions = session.getTransactions(
                accountValue,
                TransactionParams(
                    subaccount = initialTransaction.accountInjected?.pointer ?: 0,
                    confirmations = 0
                )
            )

            transactions
                .transactions
                .indexOfFirst { it.txHash == initialTransaction.txHash } // Find the index of the transaction
                .takeIf { it >= 0 }?.let { index ->
                    transactions.jsonElement?.jsonObject?.get("transactions")?.jsonArray?.getOrNull(
                        index
                    )
                }?.let {
                    Json.encodeToString(it)
                } ?: throw Exception("Couldn't find the transaction")
        }, onSuccess = {
            postSideEffect(SideEffects.Navigate(it))
        })
    }
}