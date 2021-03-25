package com.blockstream.green.database

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

import kotlinx.parcelize.Parcelize

typealias WalletId = Long

@Entity(tableName = "wallets", indices = [Index(value = ["order"]), Index(value = ["is_hardware"])])
@Parcelize
data class Wallet(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: WalletId = 0,

    @ColumnInfo(name = "name")
    var name: String,

    @ColumnInfo(name = "network")
    var network: String,

    @ColumnInfo(name = "is_recovery_confirmed")
    var isRecoveryPhraseConfirmed: Boolean = false,

    @ColumnInfo(name = "watch_only_username")
    val watchOnlyUsername: String? = null,

    @ColumnInfo(name = "is_electrum")
    val isElectrum: Boolean = false,

    @ColumnInfo(name = "is_bip39")
    val isBIP39: Boolean = false,

    @ColumnInfo(name = "is_hardware")
    val isHardware: Boolean = false,

    @ColumnInfo(name = "active_account")
    var activeAccount: Long = 0,

    @ColumnInfo(name = "order")
    val order: Int = 0,
) : Parcelable {
    val isLiquid
        get() = network.contains("liquid")

    val isWatchOnly
        get() = watchOnlyUsername != null
}