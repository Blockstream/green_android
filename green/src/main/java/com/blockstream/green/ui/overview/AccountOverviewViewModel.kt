package com.blockstream.green.ui.overview


import androidx.lifecycle.*
import com.blockstream.gdk.data.Account
import com.blockstream.gdk.data.Transaction
import com.blockstream.green.data.Countly
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.gdk.Assets
import com.blockstream.green.gdk.GdkSession
import com.blockstream.green.gdk.policyAsset
import com.blockstream.green.managers.SessionManager
import com.blockstream.green.ui.wallet.AbstractAccountWalletViewModel
import com.blockstream.green.utils.ConsumableEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach


class AccountOverviewViewModel @AssistedInject constructor(
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    countly: Countly,
    @Assisted initWallet: Wallet,
    @Assisted account: Account,
) : AbstractAccountWalletViewModel(sessionManager, walletRepository, countly, initWallet, account) {
    val isWatchOnly: LiveData<Boolean> = MutableLiveData(wallet.isWatchOnly)


    private val _transactionsLiveData: MutableLiveData<List<Transaction>> =
        MutableLiveData(listOf(Transaction.LoadingTransaction))
    val transactionsLiveData: LiveData<List<Transaction>> get() = _transactionsLiveData

    private val _transactionsPagerLiveData: MutableLiveData<Boolean?> = MutableLiveData(null)
    val transactionsPagerLiveData: LiveData<Boolean?> get() = _transactionsPagerLiveData
    val transactionsPager: Boolean? get() = _transactionsPagerLiveData.value

    private val _assetsLiveData: MutableLiveData<Assets> =
        MutableLiveData(GdkSession.AssetsLoading)
    val assetsLiveData: LiveData<Assets> get() = _assetsLiveData
    val assets: Assets get() = _assetsLiveData.value!!

    val policyAsset: Long get() = session.accountAssets(account).policyAsset()

    init {
        session
            .accountAssetsFlow(account)
            .map {
                it.takeIf { account.isLiquid && it.size > 1 } ?: emptyMap()
            }
            .onEach { assets ->
                _assetsLiveData.value = assets
            }.launchIn(viewModelScope)

        session.accountTransactionsFlow(account).onEach {
            _transactionsLiveData.value = it
        }.launchIn(viewModelScope)

        session.accountTransactionsPagerFlow(account).onEach {
            _transactionsPagerLiveData.value = it
        }.launchIn(viewModelScope)

        session.getTransactions(account = account, isReset = true, isLoadMore = false)
    }

    fun refresh() {
        session.getTransactions(account = account, isReset = false, isLoadMore = false)
        session.updateAccountsAndBalances(refresh = true, updateBalancesForAccounts = listOf(account))
        session.updateLiquidAssets()
    }

    fun loadMoreTransactions() {
        logger.info { "loadMoreTransactions" }
        session.getTransactions(account = account, isReset = false, isLoadMore = true)
    }

    fun archiveAccount() {
        super.updateAccountVisibility(account, true){
            onEvent.postValue(ConsumableEvent(NavigateEvent.NavigateBack()))
        }
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            wallet: Wallet,
            account: Account,
        ): AccountOverviewViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            wallet: Wallet,
            account: Account,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(wallet, account) as T
            }
        }
    }
}