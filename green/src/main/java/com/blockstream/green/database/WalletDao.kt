package com.blockstream.green.database

import androidx.lifecycle.LiveData
import androidx.room.*
import io.reactivex.rxjava3.core.Observable

@Dao
interface WalletDao {
    @Insert
    fun insert(wallet: Wallet) : Long

    @Delete
    fun deleteSync(wallet: Wallet)

    @Query("DELETE FROM wallets")
    fun deleteWallets()

    @Update
    fun updateSync(vararg wallet: Wallet)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(loginCredentials: LoginCredentials)

    @Update
    fun updateLoginCredentials(vararg loginCredentials: LoginCredentials)

    @Delete
    fun deleteLoginCredentials(loginCredentials: LoginCredentials)

    @Query("SELECT * FROM wallets WHERE id = :id")
    fun getWalletLiveData(id: WalletId): LiveData<Wallet>

    @Query("SELECT * FROM wallets WHERE id = :id")
    fun getWalletSync(id: WalletId): Wallet?

    @Query("SELECT * FROM wallets WHERE id = :id")
    fun getWalletObservable(id: WalletId): Observable<Wallet>

    @Query("SELECT * FROM wallets WHERE id = :id")
    suspend fun getWalletSuspend(id: WalletId): Wallet

    @Query("SELECT * FROM wallets")
    fun getWallets(): LiveData<List<Wallet>>

    @Query("SELECT * FROM wallets")
    suspend fun getWalletsSuspend(): List<Wallet>

    @Query("SELECT * FROM wallets")
    fun getWalletsSync(): List<Wallet>

    @Query("SELECT * FROM wallets WHERE is_hardware = 0")
    fun getSoftwareWallets(): LiveData<List<Wallet>>

    @Query("SELECT * FROM wallets WHERE is_hardware = 1")
    fun getHardwareWallets(): LiveData<List<Wallet>>

    // Note: This query is not indexed
    @Query("SELECT * FROM wallets WHERE network = :network")
    fun getWalletsForNetworkSync(network: String): List<Wallet>

    @Query("SELECT EXISTS(SELECT id FROM wallets LIMIT 1)")
    fun walletsExists(): LiveData<Boolean>

    @Query("SELECT EXISTS(SELECT id FROM wallets LIMIT 1)")
    suspend fun walletsExistsSuspend(): Boolean

    @Transaction
    @Query("SELECT * FROM wallets WHERE id = :id")
    fun getWalletLoginCredentials(id: WalletId): LiveData<WalletAndLoginCredentials>

    @Transaction
    @Query("SELECT * FROM wallets WHERE id = :id")
    suspend fun getWalletLoginCredentialsSuspend(id: WalletId): WalletAndLoginCredentials

    @Transaction
    @Query("SELECT * FROM wallets WHERE id = :id")
    fun getWalletLoginCredentialsObservable(id: WalletId): Observable<WalletAndLoginCredentials>

    @Query("SELECT * FROM login_credentials WHERE wallet_id = :id")
    suspend fun getLoginCredentialsSuspend(id: WalletId): List<LoginCredentials>

    @Query("SELECT * FROM login_credentials WHERE wallet_id = :id AND credential_type = :type")
    suspend fun getLoginCredentialsSuspend(id: WalletId, type: CredentialType): LoginCredentials

    @Query("DELETE FROM login_credentials WHERE wallet_id = :id AND credential_type = :type")
    fun deleteLoginCredentialsSync(id: WalletId, type: Int)

    @Query("DELETE FROM login_credentials WHERE wallet_id = :id AND credential_type = :type")
    suspend fun deleteLoginCredentialsSuspend(id: WalletId, type: Int)

    @Query("DELETE FROM login_credentials")
    fun deleteLoginCredentials()

}