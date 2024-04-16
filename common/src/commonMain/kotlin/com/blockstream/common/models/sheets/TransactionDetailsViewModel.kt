package com.blockstream.common.models.sheets

import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.previewAccountAsset
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.Transaction
import com.blockstream.common.models.GreenViewModel
import com.rickclephas.kmm.viewmodel.coroutineScope
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
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
    @NativeCoroutinesState
    abstract val data: StateFlow<List<Pair<String, String>>>
}

class TransactionDetailsViewModel(initialTransaction: Transaction, greenWallet: GreenWallet) :
    TransactionDetailsViewModelAbstract(
        greenWallet = greenWallet,
        accountAsset = initialTransaction.account.accountAsset
    ) {
    override fun screenName(): String = "TransactionDetails"

    private var _transaction = MutableStateFlow(initialTransaction)

    private val _data: MutableStateFlow<List<Pair<String, String>>> = MutableStateFlow(listOf())
    override val data = _data.asStateFlow()

    init {
        if (session.isConnected) {
            combine(
                session.walletTransactions,
                session.accountTransactions(initialTransaction.account),
                session.block(initialTransaction.account.network)
            ) { walletTransactions, accountTransactions, _ ->
                // Be sure to find the correct tx not just by hash but also with the correct type (cross-account transactions)
                walletTransactions.data()?.find { it.txHash == initialTransaction.txHash && it.txType == initialTransaction.txType }
                    ?: accountTransactions.data()?.find { it.txHash == initialTransaction.txHash }
            }.filterNotNull().onEach {
                _transaction.value = it
            }.launchIn(viewModelScope.coroutineScope)
        }

        _transaction.onEach {
            _data.value = _transaction.value.details(session)
        }.launchIn(viewModelScope.coroutineScope)


        bootstrap()
    }
}

class TransactionDetailsViewModelPreview : TransactionDetailsViewModelAbstract(
    greenWallet = previewWallet(),
    accountAsset = previewAccountAsset()
) {

    override val data: StateFlow<List<Pair<String, String>>> = MutableStateFlow(
        listOf(
            "Invoice Created" to "October 26, 2023 at 11:53:30 GMT +2",
            // "Transaction ID" to "i6783425v678i453678ib345c678ibdjihw23456789rwerufhf9y373",
            "Invoice Description" to "Empty",
            "Output" to "i6783425v678i453678ib345c678ibdjihw23456789rwerufhf9y373",
            "Payment Hash" to "i6783425v678i453678ib345c678ibdjihw23456789rwerufhf9y373r8hdsqoh8327dq23798dyq237y8dh7q27y9",
        )
    )

    companion object {
        fun preview() = TransactionDetailsViewModelPreview()
    }
}