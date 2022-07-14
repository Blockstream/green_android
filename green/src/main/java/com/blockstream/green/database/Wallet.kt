package com.blockstream.green.database

import android.os.Parcelable
import androidx.room.*
import com.blockstream.gdk.data.Network
import kotlinx.parcelize.Parcelize
import mu.KLogging

typealias WalletId = Long

@Entity(tableName = "wallets", indices = [Index(value = ["order"]), Index(value = ["is_hardware"]), Index(value = ["wallet_hash_id"])])
@Parcelize
data class Wallet constructor(
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

    @ColumnInfo(name = "ask_bip39_passphrase")
    var askForBip39Passphrase: Boolean = false,

    @ColumnInfo(name = "watch_only_username")
    val watchOnlyUsername: String? = null,

    @ColumnInfo(name = "is_hardware")
    val isHardware: Boolean = false,

    @ColumnInfo(name = "active_account")
    var activeAccount: Long = 0,

    @ColumnInfo(name = "order")
    val order: Int = 0,

    @Ignore
    val isEphemeral: Boolean = false
) : Parcelable {

    // Make Room compile by providing a constructor without the @Ignore property
    constructor(
        id: WalletId,
        walletHashId: String,
        name: String,
        network: String,
        isRecoveryPhraseConfirmed: Boolean,
        askForBip39Passphrase: Boolean,
        watchOnlyUsername: String?,
        isHardware: Boolean,
        activeAccount: Long,
        order: Int
    ) : this(
        id,
        walletHashId,
        name,
        network,
        isRecoveryPhraseConfirmed,
        askForBip39Passphrase,
        watchOnlyUsername,
        isHardware,
        activeAccount,
        order,
        false
    )

    val isLiquid
        get() = network.contains("liquid")

    val isElectrum
        get() = network.contains("electrum")

    val isWatchOnly
        get() = watchOnlyUsername != null

    val isBip39Ephemeral
        get() = isEphemeral && !isHardware

    companion object : KLogging() {
        private var ephemeralWalletIdCounter = -1L

        fun createEphemeralWallet(network: Network, isHardware: Boolean = false): Wallet {
            return Wallet(
                id = ephemeralWalletIdCounter--,
                walletHashId = network.id,
                name = network.productName,
                network = network.network,
                isRecoveryPhraseConfirmed = true,
                isHardware = isHardware,
                activeAccount = 0,
                isEphemeral = true
            )
        }
    }
}