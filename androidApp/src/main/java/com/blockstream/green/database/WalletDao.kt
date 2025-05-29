package com.blockstream.green.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface WalletDao {
    @Insert
    suspend fun insertWallet(wallet: Wallet): Long

    @Update
    suspend fun updateWallet(vararg wallet: Wallet)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceLoginCredentials(loginCredentials: LoginCredentials)

    @Update
    suspend fun updateLoginCredentials(vararg loginCredentials: LoginCredentials)

    @Delete
    suspend fun deleteLoginCredentials(loginCredentials: LoginCredentials)

    @Query("SELECT * FROM wallets WHERE is_hardware = 0")
    suspend fun getSoftwareWallets(): List<Wallet>

    @Query("SELECT * FROM wallets")
    suspend fun getAllWallets(): List<Wallet>

    @Query("SELECT * FROM login_credentials WHERE wallet_id = :id")
    suspend fun getLoginCredentialsSuspend(id: WalletId): List<LoginCredentials>
}