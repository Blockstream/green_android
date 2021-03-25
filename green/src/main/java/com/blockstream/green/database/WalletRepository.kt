package com.blockstream.green.database

import androidx.lifecycle.LiveData
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class WalletRepository @Inject constructor(private val walletDao: WalletDao) {

    fun deleteWallet(wallet: Wallet) = walletDao.deleteSync(wallet)
    fun updateWalletSync(wallet: Wallet) = walletDao.updateSync(wallet)

    fun deleteWallets() = walletDao.deleteWallets()

    fun getWalletLiveData(id: WalletId) = walletDao.getWalletLiveData(id)
    fun getWalletSync(id: WalletId) = walletDao.getWalletSync(id)

    fun getWalletObservable(id: WalletId) = walletDao.getWalletObservable(id)
    suspend fun getWalletSuspend(id: WalletId) = walletDao.getWalletSuspend(id)

    fun addWallet(wallet: Wallet) = walletDao.insert(wallet)
    fun addLoginCredentials(loginCredentials: LoginCredentials) = walletDao.insert(loginCredentials)
    fun updateLoginCredentials(loginCredentials: LoginCredentials) = walletDao.updateLoginCredentials(loginCredentials)
    fun deleteLoginCredentials(loginCredentials: LoginCredentials) = walletDao.deleteLoginCredentials(loginCredentials)

    fun getWalletLoginCredentials(id: WalletId) = walletDao.getWalletLoginCredentials(id)
    suspend fun getWalletLoginCredentialsSuspend(id: WalletId) = walletDao.getWalletLoginCredentialsSuspend(id)
    fun getWalletLoginCredentialsObservable(id: WalletId) = walletDao.getWalletLoginCredentialsObservable(id)
    suspend fun getLoginCredentialsSuspend(id: WalletId, type: CredentialType) = walletDao.getLoginCredentialsSuspend(id,type)
    fun deleteLoginCredentials() = walletDao.deleteLoginCredentials()

    fun deleteLoginCredentialsSync(walletId: WalletId, type: CredentialType) = walletDao.deleteLoginCredentialsSync(walletId, type.value)
    suspend fun deleteLoginCredentialsSuspend(walletId: WalletId, type: CredentialType) = walletDao.deleteLoginCredentialsSuspend(walletId, type.value)

    fun walletsExists() = walletDao.walletsExists()
    fun walletsExistsSuspend() = walletDao.walletsExistsSuspend()
    fun getWallets(): LiveData<List<Wallet>> = walletDao.getWallets()
    fun getWalletsSync() = walletDao.getWalletsSync()
    fun getSoftwareWallets(): LiveData<List<Wallet>> = walletDao.getSoftwareWallets()
    fun getHardwareWallets(): LiveData<List<Wallet>> = walletDao.getHardwareWallets()
}