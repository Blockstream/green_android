package com.blockstream.green.ui.overview

import androidx.lifecycle.*


import com.blockstream.gdk.*
import com.blockstream.gdk.data.SubAccount
import com.blockstream.gdk.data.Transaction
import com.blockstream.gdk.data.Transactions
import com.blockstream.gdk.data.TwoFactorReset
import com.blockstream.gdk.params.TransactionParams
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.gdk.observable
import com.blockstream.green.ui.items.AlertType
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.ui.wallet.WalletViewModel
import com.blockstream.green.utils.ConsumableEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.addTo
import kotlin.properties.Delegates


class OverviewViewModel @AssistedInject constructor(
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    @Assisted wallet: Wallet,
) : AbstractWalletViewModel(sessionManager, walletRepository, wallet) {

    enum class State {
        Overview, Account, Asset
    }

    private val state: MutableLiveData<State> = MutableLiveData(State.Overview)
    fun getState(): LiveData<State> = state

//    private val subAccount: MutableLiveData<SubAccount> = MutableLiveData()
//    fun getSubAccount(): LiveData<SubAccount> = subAccount

    private val subAccounts: MutableLiveData<List<SubAccount>> = MutableLiveData()
    fun getSubAccounts(): LiveData<List<SubAccount>> = subAccounts

    private var allBalances: Balances = linkedMapOf()
    private val balances: MutableLiveData<Balances> = MutableLiveData(linkedMapOf())
    fun getBalancesLiveData(): LiveData<Balances> = balances

    private val notifications = MutableLiveData<List<AlertType>>(listOf())
    fun getNotifications(): LiveData<List<AlertType>> = notifications

    val balancesLoading = MutableLiveData(false)

    fun isMainnet(): LiveData<Boolean> = MutableLiveData(session.isMainnet)
    fun isLiquid(): LiveData<Boolean> = MutableLiveData(wallet.isLiquid)

    private val transactions: MutableLiveData<List<Transaction>> = MutableLiveData()
    fun getTransactions(): LiveData<List<Transaction>> = transactions

    val assetsUpdated: MutableLiveData<ConsumableEvent<Boolean>> = MutableLiveData()

    val isWatchOnly: LiveData<Boolean> = MutableLiveData(wallet.isWatchOnly)

//    private var accountIndex: Long = wallet.activeAccount
    private var pendingSubAccountSwitch: Long = -1

    private var allSubAccounts: List<SubAccount> by Delegates.observable(listOf()) { _, _, newValue ->
        subAccounts.postValue(filterSubAccounts(newValue))
    }

    init {
        updateBalance()
        updateTransactions()

        session
            .getBalancesObservable()
            .subscribe {
                allBalances = it
                balances.postValue(it)
            }.addTo(disposables)

        session.getAssetsObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                assetsUpdated.postValue(ConsumableEvent(true))
            }.addTo(disposables)

        session
            .getSubAccountsObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                allSubAccounts = it

                if (pendingSubAccountSwitch >= 0) {
                    allSubAccounts.find { subAccount ->
                        subAccount.pointer == pendingSubAccountSwitch
                    }?.let { subAccount ->
                        selectSubAccount(subAccount)
                    }

                    pendingSubAccountSwitch = -1
                }
            }?.addTo(disposables)

        session
            .getTwoFactorResetObservable()
            .subscribe {
                if (it.isActive){
                    val list = mutableListOf<AlertType>()
                    if(it.isDisputed){
                        list += AlertType.Dispute2FA()
                    }else{
                        list += AlertType.Reset2FA(4)
                    }
                    notifications.postValue(list)
                }
            }.addTo(disposables)
    }

    fun refresh(){
        session.updateSubAccounts()
        updateBalance()
    }

    private fun updateTransactions(){
        session.observable {
            it.getTransactions(TransactionParams(wallet.activeAccount)).result<Transactions>()
        }.subscribe({
            transactions.value = it.transactions
        }, {
            it.printStackTrace()
        })
    }

    private fun updateBalance() {
        session.updateBalance(wallet.activeAccount)

//        session.observable {
//            it.getBalance(BalanceParams(wallet.activeAccount))
//        }.doOnSubscribe {
//            balancesLoading.value = true
//        }.doOnTerminate {
//            balancesLoading.value = false
//        }.subscribeBy(onError = {
//            // TODO find a way to show the error
//            // maybe have an error livedata to show an action like reconnect
//            it.printStackTrace()
//        }, onSuccess = {
//            allBalances = it
//            balances.postValue(allBalances)
//        }).addTo(disposables)
    }

    private fun filterSubAccounts(subAccounts: List<SubAccount>): List<SubAccount> {
        return subAccounts.filter { it.pointer != wallet.activeAccount }
    }

    fun setSubAccount(index: Long) {
        val subAccount = allSubAccounts.find {
            it.pointer == index
        }

        if (subAccount != null) {
            selectSubAccount(subAccount)
        } else {
            pendingSubAccountSwitch = index
            session.updateSubAccounts()
        }
    }

    override fun selectSubAccount(account: SubAccount) {
        super.selectSubAccount(account)

        updateBalance()
        updateTransactions()

        allSubAccounts.let {
            subAccounts.value = filterSubAccounts(it)
        }
    }

    fun setAsset(balancePair: BalancePair) {
//        this.balance.value = balancePair

        // the ordering is important
        setState(State.Asset)

        balances.value = linkedMapOf(balancePair)

//        transactions.postValue(subAccount.value!!.transactions.filter {
////            it.ticker == asset.ticker
//            it.txType == Transaction.Type.IN
//        })
    }

    fun setState(newState: State) {
        val oldState = state.value!!

        state.value = newState

        if (newState == State.Overview && oldState == State.Asset) {
            balances.value = allBalances
//            transactions.value = subAccount.value!!.transactions.subList(0, 30)
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
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return assistedFactory.create(wallet) as T
            }
        }
    }
}