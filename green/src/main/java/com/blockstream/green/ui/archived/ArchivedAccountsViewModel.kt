package com.blockstream.green.ui.archived

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.gdk.data.Account
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.rickclephas.kmm.viewmodel.coroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

@KoinViewModel
class ArchivedAccountsViewModel constructor(
    @InjectedParam wallet: GreenWallet
) : AbstractWalletViewModel(wallet) {

    private val _archivedAccountsLiveData: MutableLiveData<List<Account>> = MutableLiveData()
    val archivedAccountsLiveData: LiveData<List<Account>> get() = _archivedAccountsLiveData
    val archivedAccounts: List<Account> get() = _archivedAccountsLiveData.value ?: emptyList()

    init {
        session
            .allAccounts
            .onEach { accounts ->
                _archivedAccountsLiveData.value = accounts.filter { it.hidden }
            }.launchIn(viewModelScope.coroutineScope)
    }
}