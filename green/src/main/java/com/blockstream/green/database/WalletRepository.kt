package com.blockstream.green.database

import androidx.lifecycle.LiveData
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class WalletRepository @Inject constructor(private val walletDao: WalletDao) {
    suspend fun deleteWallet(wallet: Wallet) = walletDao.deleteWallet(wallet)
    suspend fun updateWallet(wallet: Wallet) = walletDao.updateWallet(wallet)

    fun getWalletFlow(id: WalletId) = walletDao.getWalletFlow(id)
    suspend fun getWallet(id: WalletId) = walletDao.getWallet(id)

    suspend fun getWalletWithHashId(walletHashId: String, isTestnet: Boolean, isHardware: Boolean) = walletDao.getWalletWithHashId(walletHashId, isTestnet, isHardware)

    suspend fun getWatchOnlyWalletWithHashId(walletHashId: String, network: String, isHardware: Boolean) = walletDao.getWatchOnlyWalletWithHashId(walletHashId, network, isHardware)

    suspend fun insertWallet(wallet: Wallet) = walletDao.insertWallet(wallet)

    suspend fun insertOrReplaceWallet(wallet: Wallet) = walletDao.insertOrReplaceWallet(wallet)

    suspend fun insertOrReplaceLoginCredentials(loginCredentials: LoginCredentials) = walletDao.insertOrReplaceLoginCredentials(loginCredentials)
    suspend fun updateLoginCredentials(vararg loginCredentials: LoginCredentials) = walletDao.updateLoginCredentials(*loginCredentials)

    suspend fun deleteLoginCredentials(loginCredentials: LoginCredentials) = walletDao.deleteLoginCredentials(loginCredentials)

    suspend fun getWalletLoginCredentials(id: WalletId) = walletDao.getWalletLoginCredentials(id)

    fun getWalletLoginCredentialsFlow(id: WalletId) = walletDao.getWalletLoginCredentialsFlow(id)
    suspend fun getLoginCredentialsSuspend(id: WalletId) = walletDao.getLoginCredentialsSuspend(id)
    fun deleteLoginCredentials() = walletDao.deleteLoginCredentials()

    suspend fun deleteLoginCredentials(walletId: WalletId, type: CredentialType) = walletDao.deleteLoginCredentials(walletId, type.value)

    suspend fun walletsExists(walletHashId: String, isHardware: Boolean) = walletDao.walletsExists(walletHashId, isHardware)

    suspend fun walletsExists() = walletDao.walletsExists()
    fun getSoftwareWalletsLiveData(): LiveData<List<Wallet>> = walletDao.getSoftwareWalletsLiveData()
    fun getSoftwareWalletsFlow(): Flow<List<Wallet>> = walletDao.getSoftwareWalletsFlow()
    fun getHardwareWalletsFlow(): Flow<List<Wallet>> = walletDao.getHardwareWalletsFlow()
    suspend fun getAllWallets(): List<Wallet> = walletDao.getAllWallets()

    fun getAllWalletsFlow(): Flow<List<Wallet>> = walletDao.getAllWalletsFlow()
    suspend fun getSoftwareWallets(): List<Wallet> = walletDao.getSoftwareWallets()
}