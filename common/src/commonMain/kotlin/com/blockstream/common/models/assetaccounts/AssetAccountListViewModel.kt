package com.blockstream.common.models.assetaccounts

import com.blockstream.common.data.DataState
import com.blockstream.common.data.Denomination
import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.filterForAsset
import com.blockstream.common.extensions.ifConnected
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.AccountAssetBalance
import com.blockstream.common.looks.transaction.TransactionLook
import com.blockstream.common.models.overview.WalletBalanceViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.toAmountLook
import com.blockstream.ui.events.Event
import com.blockstream.ui.navigation.NavData
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import com.rickclephas.kmp.observableviewmodel.launch
import com.rickclephas.kmp.observableviewmodel.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

abstract class AssetAccountListViewModelAbstract(
    greenWallet: GreenWallet, val assetId: String
) : WalletBalanceViewModel(greenWallet = greenWallet) {
    override fun screenName(): String = "AssetAccountList"

    @NativeCoroutinesState
    abstract val accounts: StateFlow<List<AccountAssetBalance>>

    @NativeCoroutinesState
    abstract val isLoading: StateFlow<Boolean>

    @NativeCoroutinesState
    abstract val asset: StateFlow<EnrichedAsset?>

    @NativeCoroutinesState
    abstract val totalBalance: StateFlow<String>

    @NativeCoroutinesState
    abstract val totalBalanceFiat: StateFlow<String?>

    @NativeCoroutinesState
    abstract val transactions: StateFlow<DataState<List<TransactionLook>>>
}

class AssetAccountListViewModel(
    greenWallet: GreenWallet, assetId: String
) : AssetAccountListViewModelAbstract(greenWallet = greenWallet, assetId = assetId) {

    class LocalEvents {
        class AccountClick(val accountAssetBalance: AccountAssetBalance) : Event
    }

    private val _accounts = MutableStateFlow<List<AccountAssetBalance>>(emptyList())
    override val accounts: StateFlow<List<AccountAssetBalance>> = _accounts

    private val _isLoading = MutableStateFlow(true)
    override val isLoading: StateFlow<Boolean> = _isLoading

    private val _asset = MutableStateFlow<EnrichedAsset?>(null)
    override val asset: StateFlow<EnrichedAsset?> = _asset

    private val _totalBalance = MutableStateFlow("")
    override val totalBalance: StateFlow<String> = _totalBalance

    private val _totalBalanceFiat = MutableStateFlow<String?>(null)
    override val totalBalanceFiat: StateFlow<String?> = _totalBalanceFiat

    init {
        viewModelScope.launch {
            val asset = session.walletAssets.value.data()?.assets?.get(assetId)?.let {
                EnrichedAsset.create(session, assetId)
            }

            _asset.value = asset
            _navData.value = NavData(
                title = asset?.name(session)?.toString() ?: assetId,
            )
        }
        viewModelScope.launch {
            val accountBalances = session.accounts.value.filterForAsset(assetId, session).map { account ->
                AccountAssetBalance.create(
                    accountAsset = AccountAsset.fromAccountAsset(
                        account = account, assetId = assetId, session = session
                    ), session = session
                )
            }

            _accounts.value = accountBalances
            _isLoading.value = false

            updateTotalBalance(accountBalances)


            session.ifConnected {
                combine(
                    session.accounts, session.accountsAndBalanceUpdated
                ) { accounts, _ ->
                    accounts.filterForAsset(assetId, session).map { account ->
                        AccountAssetBalance.create(
                            accountAsset = AccountAsset.fromAccountAsset(
                                account = account, assetId = assetId, session = session
                            ), session = session
                        )
                    }
                }.onEach { accountsList ->
                    _accounts.value = accountsList
                    updateTotalBalance(accountsList)
                }.launchIn(viewModelScope.coroutineScope)
            }
        }

        bootstrap()
    }

    private val _transactions: StateFlow<DataState<List<TransactionLook>>> = combine(
        session.walletTransactions.filter { session.isConnected }, session.settings()
    ) { transactions, _ ->
        transactions.mapSuccess { transactionList ->
            transactionList.filter { transaction ->
                transaction.satoshi.containsKey(assetId)
            }.map { transaction ->
                TransactionLook.create(
                    transaction = transaction, session = session, disableHideAmounts = true
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataState.Loading)

    override val transactions: StateFlow<DataState<List<TransactionLook>>> = combine(
        hideAmounts, _transactions
    ) { hideAmounts, transactionsLooks ->
        if (transactionsLooks is DataState.Success && hideAmounts) {
            DataState.Success(transactionsLooks.data.map { it.asMasked })
        } else {
            transactionsLooks
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataState.Loading)

    private fun updateTotalBalance(accountBalances: List<AccountAssetBalance>) {
        viewModelScope.launch {
            var totalSatoshi = 0L
            accountBalances.forEach { accountAssetBalance ->
                val balance = accountAssetBalance.accountAsset.balance(session)
                totalSatoshi += balance
            }

            _totalBalance.value = totalSatoshi.toAmountLook(
                session = session, assetId = assetId, withUnit = true, withGrouping = true, withMinimumDigits = false
            ) ?: "0"

            _totalBalanceFiat.value = if (totalSatoshi > 0) {
                totalSatoshi.toAmountLook(
                    session = session, assetId = assetId, withUnit = true, denomination = Denomination.fiat(session)
                )
            } else null
        }
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is LocalEvents.AccountClick -> {
                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.AssetAccountDetails(
                            accountAsset = event.accountAssetBalance.accountAsset, greenWallet = greenWallet
                        )
                    )
                )
            }
        }
    }
}

class AssetAccountListViewModelPreview(
    greenWallet: GreenWallet, assetId: String
) : AssetAccountListViewModelAbstract(greenWallet = greenWallet, assetId = assetId) {

    override val accounts: StateFlow<List<AccountAssetBalance>> = MutableStateFlow(emptyList())
    override val isLoading: StateFlow<Boolean> = MutableStateFlow(false)
    override val asset: StateFlow<EnrichedAsset?> = MutableStateFlow(EnrichedAsset.PreviewBTC)
    override val totalBalance: StateFlow<String> = MutableStateFlow("0.0002821 BTC")
    override val totalBalanceFiat: StateFlow<String?> = MutableStateFlow("US$ 2,321.00")
    override val transactions: StateFlow<DataState<List<TransactionLook>>> = MutableStateFlow(DataState.Empty)

    companion object {
        fun preview() = AssetAccountListViewModelPreview(
            greenWallet = previewWallet(), assetId = "btc"
        )
    }
}