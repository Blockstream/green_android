package com.blockstream.green.ui.archived

import androidx.lifecycle.*
import com.blockstream.gdk.data.Account
import com.blockstream.green.data.Countly
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.managers.SessionManager
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ArchivedAccountsViewModel @AssistedInject constructor(
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    countly: Countly,
    @Assisted wallet: Wallet
) : AbstractWalletViewModel(sessionManager, walletRepository, countly, wallet) {

    private val _archivedAccountsLiveData: MutableLiveData<List<Account>> = MutableLiveData()
    val archivedAccountsLiveData: LiveData<List<Account>> get() = _archivedAccountsLiveData
    val archivedAccounts: List<Account> get() = _archivedAccountsLiveData.value ?: emptyList()

    init {
        session
            .allAccountsFlow
            .onEach { accounts ->
                _archivedAccountsLiveData.value = accounts.filter { it.hidden }
            }.launchIn(viewModelScope)
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            wallet: Wallet
        ): ArchivedAccountsViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            wallet: Wallet
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(wallet) as T
            }
        }
    }
}