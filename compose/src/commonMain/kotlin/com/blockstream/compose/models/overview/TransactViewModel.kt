package com.blockstream.compose.models.overview

import androidx.lifecycle.viewModelScope
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_transact
import com.blockstream.common.data.DataState
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.extensions.previewTransactionLook
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.Transaction
import com.blockstream.common.gdk.data.toTransaction
import com.blockstream.common.looks.transaction.TransactionLook
import com.blockstream.compose.navigation.NavData
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.green.domain.base.Result
import com.blockstream.green.domain.meld.GetPendingMeldTransactions
import com.blockstream.green.utils.Loggable
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.koin.core.component.inject

abstract class TransactViewModelAbstract(
    greenWallet: GreenWallet
) : WalletBalanceViewModel(greenWallet = greenWallet) {

    override fun screenName(): String = "TransactTab"
    abstract val transactions: StateFlow<DataState<List<TransactionLook>>>

    fun buy() {
        countly.buyInitiate()
        postEvent(
            NavigateDestinations.Buy(
                greenWallet = greenWallet
            )
        )
        countly.buyInitiate()
    }
}

class TransactViewModel(greenWallet: GreenWallet) :
    TransactViewModelAbstract(greenWallet = greenWallet) {
    
    private val getPendingMeldTransactions: GetPendingMeldTransactions by inject()
    private var refreshJob: Job? = null
    
    override fun segmentation(): HashMap<String, Any> =
        countly.sessionSegmentation(session = session)

    private val _meldTransactions: StateFlow<List<Transaction>> =
        greenWallet.xPubHashId.let {
            combine(
                getPendingMeldTransactions.observe(),
                session.accounts
            ) { result, accounts ->
                when (result) {
                    is Result.Success -> {
                        val bitcoinAccount = accounts.firstOrNull { it.isBitcoin && !it.isLightning }
                        if (bitcoinAccount != null) {
                            result.data.mapNotNull { it.toTransaction(bitcoinAccount) }
                        } else {
                            emptyList()
                        }
                    }
                    else -> emptyList()
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())
        }

    private val _transactions: StateFlow<DataState<List<TransactionLook>>> = combine(
        session.walletTransactions.filter { session.isConnected },
        session.settings(),
        _meldTransactions
    ) { transactions, _, meldTransactions ->
        transactions.mapSuccess { gdkTransactions ->
            val allTransactions = (gdkTransactions + meldTransactions)
                .distinctBy { it.txHash }
                .sortedByDescending { it.createdAtTs }
            
            allTransactions.map {
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
        greenWalletFlow.filterNotNull().onEach {
            updateNavData(it)
        }.launchIn(this)

        viewModelScope.launch {
            updateNavData(greenWallet)
        }

        getPendingMeldTransactions()
        
        startPeriodicRefresh()

        bootstrap()
    }
    
    private fun startPeriodicRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (isActive) {
                delay(60_000L)
                getPendingMeldTransactions()
            }
        }
    }
    
    private fun getPendingMeldTransactions() {
        greenWallet.xPubHashId.let { xPubHashId ->
            viewModelScope.launch {
                getPendingMeldTransactions(
                    GetPendingMeldTransactions.Params(
                        externalCustomerId = xPubHashId
                    )
                )
            }
        }
    }
    
    override fun onCleared() {
        refreshJob?.cancel()
        super.onCleared()
    }

    private suspend fun updateNavData(greenWallet: GreenWallet) {
        _navData.value = NavData(
            title = getString(Res.string.id_transact),
            walletName = greenWallet.name,
            showBadge = !greenWallet.isRecoveryConfirmed,
            showBottomNavigation = true
        )
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