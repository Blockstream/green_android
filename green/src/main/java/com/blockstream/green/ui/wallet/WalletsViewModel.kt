package com.blockstream.green.ui.wallet

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.blockstream.green.data.Countly
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.managers.SessionManager
import com.blockstream.green.ui.AppViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class WalletsViewModel @Inject constructor(
    val walletRepository: WalletRepository,
    val sessionManager: SessionManager,
    countly: Countly
) : AppViewModel(countly) {
    val softwareWalletsLiveData: LiveData<List<Wallet>> = walletRepository.getSoftwareWalletsLiveData()

    val ephemeralWalletsLiveData = sessionManager.ephemeralWallets

    private val _hardwareWalletsLiveData: MutableLiveData<List<Wallet>> = MutableLiveData()
    val hardwareWalletsLiveData: LiveData<List<Wallet>> get() = _hardwareWalletsLiveData

    init {
        combine(walletRepository.getHardwareWalletsFlow(), sessionManager.hardwareWallets.asFlow()) { w1, w2 ->
            (w1 + w2).distinctBy { it.id }
        }.onEach {
            _hardwareWalletsLiveData.value = it
        }.launchIn(viewModelScope)
    }

    fun deleteWallet(wallet: Wallet) {
        deleteWallet(wallet, sessionManager, walletRepository, countly)
    }

    fun renameWallet(name: String, wallet: Wallet) {
        renameWallet(name, wallet, walletRepository, countly)
    }
}