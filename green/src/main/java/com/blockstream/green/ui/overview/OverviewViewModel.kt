package com.blockstream.green.ui.overview


import androidx.lifecycle.*
import com.blockstream.gdk.BalanceLoading
import com.blockstream.gdk.BalancePair
import com.blockstream.gdk.Balances
import com.blockstream.gdk.data.Block
import com.blockstream.gdk.data.SubAccount
import com.blockstream.gdk.data.Transaction
import com.blockstream.green.data.Countly
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.ui.items.AlertType
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.utils.isNotBlank
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import kotlin.properties.Delegates


class OverviewViewModel @AssistedInject constructor(
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    countly: Countly,
    @Assisted initWallet: Wallet,
) : AbstractWalletViewModel(sessionManager, walletRepository, countly, initWallet) {

    enum class State {
        Overview, Account, Asset
    }

    val isWatchOnly: LiveData<Boolean> = MutableLiveData(wallet.isWatchOnly)

    val systemMessage: MutableLiveData<AlertType?> = MutableLiveData()
    private val twoFactorState: MutableLiveData<AlertType?> = MutableLiveData()

    private val _alerts: LiveData<List<AlertType>> by lazy {
        MediatorLiveData<List<AlertType>>().apply {
            val combine = { _: Any? ->
                value = listOfNotNull(
                    twoFactorState.value,
                    systemMessage.value,
                    if (wallet.isEphemeral && !wallet.isHardware) AlertType.EphemeralBip39 else null,
                    if (session.isTestnet) AlertType.TestnetWarning else null
                )
            }
            addSource(twoFactorState, combine)
            addSource(systemMessage, combine)
        }
    }
    
    private val state: MutableLiveData<State> = MutableLiveData(State.Overview)
    private val filteredSubAccounts: MutableLiveData<List<SubAccount>> = MutableLiveData()
    private val archivedAccounts = MutableLiveData(0)

    private var allBalances: Balances = linkedMapOf(BalanceLoading)
    private val shownBalances: MutableLiveData<Balances> = MutableLiveData(allBalances)
    private val transactions: MutableLiveData<List<Transaction>> = MutableLiveData(listOf(
        Transaction.LoadingTransaction))

    private val selectedAsset = MutableLiveData<String?>()

    private val block: MutableLiveData<Block> = MutableLiveData()

    fun getState(): LiveData<State> = state
    fun getFilteredSubAccounts(): LiveData<List<SubAccount>> = filteredSubAccounts
    fun getArchivedAccounts(): LiveData<Int> = archivedAccounts
    fun getBalancesLiveData(): LiveData<Balances> = shownBalances
    fun getAlerts(): LiveData<List<AlertType>> = _alerts
    fun isMainnet(): LiveData<Boolean> = MutableLiveData(session.isMainnet)
    fun isLiquid(): LiveData<Boolean> = MutableLiveData(wallet.isLiquid)
    fun getTransactions(): LiveData<List<Transaction>> = transactions
    fun getBlock(): LiveData<Block> = block

    fun getSelectedAsset(): LiveData<String?> = selectedAsset

    private var pendingSubAccountSwitch: Long = -1

    private var allSubAccounts: List<SubAccount> by Delegates.observable(listOf()) { _, _, newValue ->
        filteredSubAccounts.value = filterSubAccounts(newValue)
    }

    var initialScrollToTop = false

    init {
        session.getBlockObservable().subscribe {
            block.postValue(it)
        }.addTo(disposables)

        session
            .getBalancesObservable()
            .subscribe {
                allBalances = it
                shownBalances.postValue(it)
            }.addTo(disposables)

        session
            .getTransationsObservable()
            .subscribe {
                transactions.postValue(it)
            }.addTo(disposables)

        session
            .getSubAccountsObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                allSubAccounts = it

                archivedAccounts.value = it.count { it.hidden }

                if (pendingSubAccountSwitch >= 0) {
                    allSubAccounts.find { subAccount ->
                        subAccount.pointer == pendingSubAccountSwitch
                    }?.let { subAccount ->
                        selectSubAccount(subAccount)
                    }

                    pendingSubAccountSwitch = -1
                }
            }.addTo(disposables)

        session
            .getSystemMessageObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if(it.isNotBlank()) {
                    systemMessage.postValue(AlertType.SystemMessage(it))
                }else{
                    systemMessage.postValue(null)
                }
            }.addTo(disposables)

        session
            .getTwoFactorResetObservable()
            .subscribe {
                twoFactorState.postValue(
                    if (it.isActive == true) {
                        if (it.isDisputed == true) {
                            AlertType.Dispute2FA(it)
                        } else {
                            AlertType.Reset2FA(it)
                        }
                    } else {
                        null
                    }
                )
            }.addTo(disposables)
    }

    fun refresh(){
        session.updateSubAccountsAndBalances(refresh = true)
        session.updateTransactionsAndBalance(isReset = false, isLoadMore = false)
        session.updateLiquidAssets()
    }

    private fun filterSubAccounts(subAccounts: List<SubAccount>): List<SubAccount> {
        return subAccounts.filter { it.pointer != session.activeAccount && !it.hidden}
    }

    fun setSubAccount(index: Long) {
        val subAccount = allSubAccounts.find {
            it.pointer == index
        }

        if (subAccount != null) {
            selectSubAccount(subAccount)
        } else {
            pendingSubAccountSwitch = index
            session.updateSubAccountsAndBalances()
        }
    }

    override fun selectSubAccount(account: SubAccount) {
        super.selectSubAccount(account)

        allBalances = linkedMapOf(BalanceLoading)
        shownBalances.postValue(allBalances)

        filteredSubAccounts.value = filterSubAccounts(allSubAccounts)
    }

    fun setAsset(balancePair: BalancePair) {
        // the ordering is important
        setState(State.Asset)
        selectedAsset.value = balancePair.first
    }

    fun setState(newState: State) {
        state.value = newState
    }

    fun loadMoreTransactions(): Boolean {
        logger.info { "loadMoreTransactions" }
        return session.updateTransactionsAndBalance(isReset = false, isLoadMore = true)
    }

    fun archiveSubAccount(subAccount: SubAccount) {
        super.updateSubAccountVisibility(subAccount, true){
            selectSubAccount(
                allSubAccounts.find { !it.hidden && it.pointer != subAccount.pointer } ?: allSubAccounts.first()
            )
        }
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            wallet: Wallet,
        ): OverviewViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            wallet: Wallet,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(wallet) as T
            }
        }
    }
}