package com.blockstream.compose.models.sheets

import androidx.lifecycle.viewModelScope
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.gdk.data.Transaction
import com.blockstream.compose.extensions.details
import com.blockstream.compose.extensions.previewAccountAsset
import com.blockstream.compose.extensions.previewWallet
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.utils.StringHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

abstract class TransactionDetailsViewModelAbstract(
    greenWallet: GreenWallet,
    accountAsset: AccountAsset
) : GreenViewModel(
    greenWalletOrNull = greenWallet,
    accountAssetOrNull = accountAsset
) {
    abstract val data: StateFlow<List<Pair<StringHolder, StringHolder>>>
}

class TransactionDetailsViewModel(greenWallet: GreenWallet, initialTransaction: Transaction) :
    TransactionDetailsViewModelAbstract(
        greenWallet = greenWallet,
        accountAsset = initialTransaction.account.accountAsset
    ) {
    override fun screenName(): String = "TransactionDetails"

    private var _transaction = MutableStateFlow(initialTransaction)

    private val _data: MutableStateFlow<List<Pair<StringHolder, StringHolder>>> =
        MutableStateFlow(listOf())
    override val data = _data.asStateFlow()

    init {
        if (session.isConnected) {
            combine(
                session.walletTransactions,
                session.accountTransactions(initialTransaction.account),
                session.block(initialTransaction.account.network)
            ) { walletTransactions, accountTransactions, _ ->
                // Be sure to find the correct tx not just by hash but also with the correct type (cross-account transactions)
                walletTransactions.data()
                    ?.find { it.txHash == initialTransaction.txHash && it.txType == initialTransaction.txType }
                    ?: accountTransactions.data()?.find { it.txHash == initialTransaction.txHash }
            }.filterNotNull().onEach {
                _transaction.value = it
            }.launchIn(viewModelScope)
        }

        _transaction.onEach {
            _data.value = _transaction.value.details(session = session, database = database)
        }.launchIn(viewModelScope)

        bootstrap()
    }
}

class TransactionDetailsViewModelPreview : TransactionDetailsViewModelAbstract(
    greenWallet = previewWallet(),
    accountAsset = previewAccountAsset()
) {

    override val data: StateFlow<List<Pair<StringHolder, StringHolder>>> = MutableStateFlow(
        listOf(
            StringHolder.create("Invoice Created") to StringHolder.create("October 26, 2023 at 11:53:30 GMT +2"),
            // "Transaction ID" to "i6783425v678i453678ib345c678ibdjihw23456789rwerufhf9y373",
            StringHolder.create("Invoice Description") to StringHolder.create("Empty"),
            StringHolder.create("Output") to StringHolder.create("i6783425v678i453678ib345c678ibdjihw23456789rwerufhf9y373"),
            StringHolder.create("Payment Hash") to StringHolder.create("i6783425v678i453678ib345c678ibdjihw23456789rwerufhf9y373r8hdsqoh8327dq23798dyq237y8dh7q27y9"),
        )
    )

    companion object {
        fun preview() = TransactionDetailsViewModelPreview()
    }
}