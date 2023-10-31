package com.blockstream.common.models.transaction

import com.blockstream.common.data.GreenWallet
import com.blockstream.common.events.Event
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.Transaction
import com.blockstream.common.gdk.params.TransactionParams
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffects
import com.rickclephas.kmm.viewmodel.stateIn
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

abstract class TransactionDetailsViewModelAbstract(
    transaction: Transaction,
    greenWallet: GreenWallet
) : GreenViewModel(
    greenWalletOrNull = greenWallet,
    accountAssetOrNull = AccountAsset.fromAccount(transaction.account)
) {

    override fun screenName(): String = "TransactionDetails"

    @NativeCoroutinesState
    abstract val transaction: StateFlow<Transaction>

    @NativeCoroutinesState
    abstract val memo: MutableStateFlow<String>
}

class TransactionDetailsViewModel(transaction: Transaction, greenWallet: GreenWallet) :
    TransactionDetailsViewModelAbstract(transaction = transaction, greenWallet = greenWallet) {

    class LocalEvents {
        class SaveMemo : Event
        class BumpFee : Event
    }

    override val transaction: StateFlow<Transaction>

    override val memo: MutableStateFlow<String> = MutableStateFlow(transaction.memo)

    init {

        // Update transaction data and create a copy with a stable memo so that we can properly animate
        this.transaction = combine(
            session.walletTransactions,
            session.accountTransactions(transaction.account),
            session.block(transaction.account.network)
        ) { walletTransactions, accountTransactions, _ ->
            // Be sure to find the correct tx not just by hash but also with the correct type (cross-account transactions)
            walletTransactions.find { it.txHash == transaction.txHash && it.txType == transaction.txType }
                ?: accountTransactions.find { it.txHash == transaction.txHash }
        }.map {
            stabilizeTransaction(it ?: transaction)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            stabilizeTransaction(transaction)
        )
    }

    override fun handleEvent(event: Event) {
        super.handleEvent(event)

        if (event is LocalEvents.SaveMemo) {
            doAsync({
                session.setTransactionMemo(
                    network = transaction.value.account.network,
                    txHash = transaction.value.txHash,
                    memo = memo.value
                )

                // update transaction
                session.getTransactions(
                    account = transaction.value.account,
                    isReset = false,
                    isLoadMore = false
                )
                session.updateWalletTransactions(updateForAccounts = listOf(transaction.value.account))
            }, onSuccess = {

            })
        } else if (event is LocalEvents.BumpFee) {
            doAsync({
                val transactions = session.getTransactions(
                    transaction.value.account,
                    TransactionParams(
                        subaccount = transaction.value.account.pointer,
                        confirmations = 0
                    )
                )

                transactions
                    .transactions
                    .indexOfFirst { it.txHash == transaction.value.txHash } // Find the index of the transaction
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

    private fun stabilizeTransaction(tx: Transaction): Transaction {
        return tx.copy(memo = "STABLE_FOR_EQUALS").also {
            it.accountInjected = tx.accountInjected
        }
    }
}