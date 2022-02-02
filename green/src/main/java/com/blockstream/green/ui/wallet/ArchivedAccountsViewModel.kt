package com.blockstream.green.ui.wallet

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.blockstream.gdk.data.SubAccount
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.gdk.SessionManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo

class ArchivedAccountsViewModel @AssistedInject constructor(
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    @Assisted wallet: Wallet,
) : AbstractWalletViewModel(sessionManager, walletRepository, wallet) {

    private val archivedSubAccountsLiveData: MutableLiveData<List<SubAccount>> = MutableLiveData()
    fun getArchivedSubAccountsLiveData(): LiveData<List<SubAccount>> = archivedSubAccountsLiveData

    init {
        session
            .getSubAccountsObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                archivedSubAccountsLiveData.value = it.filter { it.hidden }
            }.addTo(disposables)
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            wallet: Wallet,
        ): ArchivedAccountsViewModel
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