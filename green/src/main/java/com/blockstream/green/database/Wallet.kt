package com.blockstream.green.database

import android.os.Parcelable
import androidx.room.*
import kotlinx.parcelize.Parcelize
import mu.KLogging

typealias WalletId = Long

@Entity(tableName = "wallets", indices = [Index(value = ["order"]), Index(value = ["is_hardware"]), Index(value = ["wallet_hash_id"])])
@Parcelize
@TypeConverters(Converters::class)
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

    @ColumnInfo(name = "is_recovery_confirmed")
    val isRecoveryPhraseConfirmed: Boolean = true,

    @ColumnInfo(name = "ask_bip39_passphrase")
    var askForBip39Passphrase: Boolean = false,

    @ColumnInfo(name = "watch_only_username")
    val watchOnlyUsername: String? = null,

    @ColumnInfo(name = "is_hardware")
    val isHardware: Boolean = false,

    @ColumnInfo(name = "is_testnet")
    var isTestnet: Boolean,

    @ColumnInfo(name = "network") // kept it as network for backward compatibility
    var activeNetwork: String,

    @ColumnInfo(name = "active_account")
    var activeAccount: Long = 0,

    @ColumnInfo(name = "order")
    val order: Int = 0,

    @Ignore
    var isEphemeral: Boolean = false,

    @Ignore
    var ephemeralId: Long = 0L
) : Parcelable {

    // Make Room compile by providing a constructor without the @Ignore property
    constructor(
        id: WalletId,
        walletHashId: String,
        name: String,
        isRecoveryPhraseConfirmed: Boolean,
        askForBip39Passphrase: Boolean,
        watchOnlyUsername: String?,
        isHardware: Boolean,
        isTestnet: Boolean,
        activeNetwork: String,
        activeAccount: Long,
        order: Int,
    ) : this(
        id,
        walletHashId,
        name,
        isRecoveryPhraseConfirmed,
        askForBip39Passphrase,
        watchOnlyUsername,
        isHardware,
        isTestnet,
        activeNetwork,
        activeAccount,
        order,
        false,
        0L
    )

    val isMainnet
        get() = !isTestnet

    val isWatchOnly
        get() = watchOnlyUsername != null

    val isBip39Ephemeral
        get() = isEphemeral && !isHardware

    val ephemeralBip39Name
        get() = "BIP39 #${ephemeralId}"

    companion object : KLogging() {
        private var ephemeralWalletIdCounter = -1L

        fun createEphemeralWallet(ephemeralId: Long, networkId: String, name: String? = null, isHardware: Boolean = false, isTestnet: Boolean = false): Wallet {
            return Wallet(
                id = ephemeralWalletIdCounter--,
                walletHashId = networkId,
                name = name ?: networkId.replaceFirstChar { n -> n.titlecase() },
                isRecoveryPhraseConfirmed = true,
                isHardware = isHardware,
                isTestnet = isTestnet,
                activeNetwork = networkId,
                activeAccount = 0,
            ).also {
                it.isEphemeral = true
                it.ephemeralId = ephemeralId
            }
        }
    }
}
