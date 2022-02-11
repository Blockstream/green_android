package com.blockstream.green.ui

import androidx.lifecycle.LiveData
import com.blockstream.green.data.Countly
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.gdk.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WalletListCommonViewModel @Inject constructor(
    val walletRepository: WalletRepository,
    val sessionManager: SessionManager,
    val countly: Countly
) : AppViewModel() {
    val wallets: LiveData<List<Wallet>> = walletRepository.getWalletsLiveData()

    fun deleteWallet(wallet: Wallet) {
        deleteWallet(wallet, sessionManager, walletRepository, countly)
    }
}