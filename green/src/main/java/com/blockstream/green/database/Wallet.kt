package com.blockstream.green.database

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.blockstream.gdk.data.Network
import kotlinx.parcelize.Parcelize
import mu.KLogging

typealias WalletId = Long

@Entity(tableName = "wallets", indices = [Index(value = ["order"]), Index(value = ["is_hardware"]), Index(value = ["wallet_hash_id"])])
@Parcelize
data class Wallet(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: WalletId = 0,

    // defaultValue used for AutoMigration
    // Don't make it unique as a user can have a mnemonic used both as a software wallet and in a hww device
    @ColumnInfo(name = "wallet_hash_id", defaultValue = "")
    var walletHashId: String,

    @ColumnInfo(name = "name")
    var name: String,

    @ColumnInfo(name = "network")
    val network: String,

    @ColumnInfo(name = "is_recovery_confirmed")
    val isRecoveryPhraseConfirmed: Boolean = true,

    @ColumnInfo(name = "watch_only_username")
    val watchOnlyUsername: String? = null,

    @ColumnInfo(name = "is_hardware")
    val isHardware: Boolean = false,

    @ColumnInfo(name = "active_account")
    var activeAccount: Long = 0,

    @ColumnInfo(name = "order")
    val order: Int = 0,
) : Parcelable {
    val isLiquid
        get() = network.contains("liquid")

    val isElectrum
        get() = network.contains("electrum")

    val isWatchOnly
        get() = watchOnlyUsername != null

    companion object : KLogging() {
        private var hardwareWalletIdCounter = -1L

        fun createEmulatedHardwareWallet(network: Network): Wallet {
            return Wallet(
                id = hardwareWalletIdCounter--,
                walletHashId = network.id,
                name = network.productName,
                network = network.network,
                isRecoveryPhraseConfirmed = true,
                isHardware = true,
                activeAccount = 0
            )
        }
    }
}