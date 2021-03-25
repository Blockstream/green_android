package com.blockstream.green.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(val walletRepository: WalletRepository) : AppViewModel() {
    val buildVersion = MutableLiveData("")

    val wallets: LiveData<List<Wallet>> = walletRepository.getWallets()
//    val softwareWallets: LiveData<List<Wallet>> = walletRepository.getSoftwareWallets()
//    val hardwareWallets: LiveData<List<Wallet>> = walletRepository.getHardwareWallets()
}