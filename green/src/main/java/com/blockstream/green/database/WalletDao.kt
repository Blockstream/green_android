package com.blockstream.green.database

import androidx.lifecycle.LiveData
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Insert
    suspend fun insertWallet(wallet: Wallet) : Long

    @Delete
    suspend fun deleteWallet(wallet: Wallet)

    @Delete
    fun deleteSync(wallet: Wallet)

    @Query("DELETE FROM wallets")
    fun deleteWallets()

    @Update
    suspend fun updateWallet(vararg wallet: Wallet)


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceLoginCredentials(loginCredentials: LoginCredentials)

    @Update
    suspend fun updateLoginCredentials(vararg loginCredentials: LoginCredentials)

    @Delete
    suspend fun deleteLoginCredentials(loginCredentials: LoginCredentials)

    @Query("SELECT * FROM wallets WHERE id = :id")
    fun getWalletFlow(id: WalletId): Flow<Wallet>

    @Query("SELECT * FROM wallets WHERE id = :id")
    suspend fun getWallet(id: WalletId): Wallet?

    @Query("SELECT * FROM wallets WHERE wallet_hash_id = :walletHashId AND is_hardware = :isHardware LIMIT 1")
    suspend fun getWalletWithHashId(walletHashId: String, isHardware: Boolean): Wallet?

    @Query("SELECT * FROM wallets WHERE is_hardware = 0")
    fun getSoftwareWalletsLiveData(): LiveData<List<Wallet>>

    @Query("SELECT * FROM wallets WHERE is_hardware = 0")
    fun getSoftwareWalletsFlow(): Flow<List<Wallet>>


    @Query("SELECT * FROM wallets WHERE is_hardware = 1")
    fun getHardwareWalletsFlow(): Flow<List<Wallet>>

    @Query("SELECT * FROM wallets")
    suspend fun getAllWallets(): List<Wallet>

    @Query("SELECT * FROM wallets WHERE is_hardware = 0")
    suspend fun getSoftwareWallets(): List<Wallet>

    @Query("SELECT EXISTS(SELECT id FROM wallets WHERE wallet_hash_id = :walletHashId AND is_hardware = :isHardware LIMIT 1)")
    suspend fun walletsExists(walletHashId: String, isHardware: Boolean): Boolean

    @Query("SELECT EXISTS(SELECT id FROM wallets LIMIT 1)")
    suspend fun walletsExists(): Boolean

    @Transaction
    @Query("SELECT * FROM wallets WHERE id = :id")
    fun getWalletLoginCredentials(id: WalletId): LiveData<WalletAndLoginCredentials>

    @Transaction
    @Query("SELECT * FROM wallets WHERE id = :id")
    suspend fun getWalletLoginCredentialsSuspend(id: WalletId): WalletAndLoginCredentials

    @Transaction
    @Query("SELECT * FROM wallets WHERE id = :id")
    fun getWalletLoginCredentialsFlow(id: WalletId): Flow<WalletAndLoginCredentials>

    @Query("SELECT * FROM login_credentials WHERE wallet_id = :id")
    suspend fun getLoginCredentialsSuspend(id: WalletId): List<LoginCredentials>

    @Query("DELETE FROM login_credentials WHERE wallet_id = :id AND credential_type = :type")
    suspend fun deleteLoginCredentials(id: WalletId, type: Int)

    @Query("DELETE FROM login_credentials")
    fun deleteLoginCredentials()
}