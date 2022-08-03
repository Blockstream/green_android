package com.blockstream.green.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.blockstream.gdk.data.AccountType
import com.blockstream.gdk.data.Network
import com.blockstream.green.data.Countly
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.managers.SessionManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

class ReviewAddAccountViewModel @AssistedInject constructor(
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    countly: Countly,
    @Assisted wallet: Wallet,
    @Assisted val accountType: AccountType?,
    @Assisted val network: Network?,
    @Assisted("mnemonic") val mnemonic: String?,
    @Assisted("xpub") val xpub: String?,
) : AbstractAddAccountViewModel(sessionManager, walletRepository, countly, wallet) {

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            wallet: Wallet,
            network: Network?,
            accountType: AccountType?,
            @Assisted("mnemonic")
            mnemonic: String?,
            @Assisted("xpub")
            xpub: String?
        ): ReviewAddAccountViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            wallet: Wallet,
            network: Network?,
            accountType: AccountType?,
            mnemonic: String?,
            xpub: String?
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(wallet, network, accountType, mnemonic, xpub) as T
            }
        }
    }
}