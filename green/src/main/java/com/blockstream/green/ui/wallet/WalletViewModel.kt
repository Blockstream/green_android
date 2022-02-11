package com.blockstream.green.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.blockstream.green.data.Countly
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.gdk.SessionManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/*
 * This is an implementation of AbstractWalletViewModel so that can easily be used by fragments without
 * needing to implement their own VM. Add any required methods to the Abstracted class.
 */
class WalletViewModel @AssistedInject constructor(
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    countly: Countly,
    @Assisted wallet: Wallet,
) : AbstractWalletViewModel(sessionManager, walletRepository, countly, wallet) {

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            wallet: Wallet
        ): WalletViewModel
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