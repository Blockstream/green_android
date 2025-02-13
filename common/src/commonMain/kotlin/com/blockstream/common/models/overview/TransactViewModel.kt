package com.blockstream.common.models.overview

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_lightning_account
import blockstream_green.common.generated.resources.id_transact
import com.blockstream.common.data.DataState
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.NavData
import com.blockstream.common.extensions.ifConnected
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.extensions.previewTransactionLook
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.looks.transaction.TransactionLook
import com.blockstream.common.utils.Loggable
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.launch
import com.rickclephas.kmp.observableviewmodel.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach
import org.jetbrains.compose.resources.getString


abstract class TransactViewModelAbstract(
    greenWallet: GreenWallet
) : WalletBalanceViewModel(greenWallet = greenWallet) {

    override fun screenName(): String = "Transact"

    @NativeCoroutinesState
    abstract val transactions: StateFlow<DataState<List<TransactionLook>>>
}

class TransactViewModel(greenWallet: GreenWallet) :
    TransactViewModelAbstract(greenWallet = greenWallet) {
    override fun segmentation(): HashMap<String, Any> =
        countly.sessionSegmentation(session = session)

    private val _transactions: StateFlow<DataState<List<TransactionLook>>> = combine(
        session.walletTransactions.filter { session.isConnected },
        session.settings()
    ) { transactions, _ ->
        transactions.mapSuccess {
            it.map {
                TransactionLook.create(
                    transaction = it,
                    session = session,
                    disableHideAmounts = true
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataState.Loading)

    // Re-calculate if needed (hideAmount or denomination & exchange rate change)
    override val transactions: StateFlow<DataState<List<TransactionLook>>> =
        combine(
            hideAmounts,
            _transactions
        ) { hideAmounts, transactionsLooks ->
            if (transactionsLooks is DataState.Success && hideAmounts) {
                DataState.Success(transactionsLooks.data.map { it.asMasked })
            } else {
                transactionsLooks
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataState.Loading)

    init {
        viewModelScope.launch {
            _navData.value = NavData(
                title = getString(Res.string.id_transact)
            )
        }

        session.ifConnected {
            greenWalletFlow.filterNotNull().onEach { greenWallet ->
                _navData.value = NavData(
                    title = greenWallet.name,
                    subtitle = if (session.isLightningShortcut) getString(Res.string.id_lightning_account) else null,
                )
            }.launchIn(this)
        }

        bootstrap()
    }
}

class TransactViewModelPreview(val isEmpty: Boolean = false) :
    TransactViewModelAbstract(greenWallet = previewWallet()) {

    override val transactions: StateFlow<DataState<List<TransactionLook>>> = MutableStateFlow(
        DataState.Success(
            if (isEmpty) listOf() else listOf(
                previewTransactionLook(),
                previewTransactionLook(),
                previewTransactionLook(),
                previewTransactionLook(),
                previewTransactionLook(),
                previewTransactionLook(),
                previewTransactionLook(),
                previewTransactionLook(),
                previewTransactionLook(),
                previewTransactionLook(),
                previewTransactionLook(),
                previewTransactionLook(),
            )
        )
    )

    companion object : Loggable() {
        fun create(isEmpty: Boolean = false) = TransactViewModelPreview(isEmpty = isEmpty)
    }
}