package com.blockstream.green.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.blockstream.data.data.DeviceIdentifier

typealias WalletId = Long

@Entity(tableName = "wallets", indices = [Index(value = ["order"]), Index(value = ["is_hardware"]), Index(value = ["wallet_hash_id"])])
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

    @ColumnInfo(name = "watch_only_username") // kept it as watch_only_username for backward compatibility
    val watchOnlyUsername: String? = null,

    @ColumnInfo(name = "is_hardware")
    val isHardware: Boolean = false,

    @ColumnInfo(name = "is_testnet")
    var isTestnet: Boolean,

    @ColumnInfo(name = "is_lightning")
    val isLightning: Boolean = false,

    @ColumnInfo(name = "network") // kept it as network for backward compatibility
    var activeNetwork: String,

    @ColumnInfo(name = "active_account")
    var activeAccount: Long = 0,

    @ColumnInfo(name = "device_identifiers")
    var deviceIdentifiers: List<DeviceIdentifier>? = null,

    @ColumnInfo(name = "order")
    var order: Long = 0,

    @Ignore
    var isEphemeral: Boolean = false,

    @Ignore
    var ephemeralId: Long = 0L
) {

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
        isLightning: Boolean,
        activeNetwork: String,
        activeAccount: Long,
        deviceIdentifiers: List<DeviceIdentifier>?,
        order: Long,
    ) : this(
        id,
        walletHashId,
        name,
        isRecoveryPhraseConfirmed,
        askForBip39Passphrase,
        watchOnlyUsername,
        isHardware,
        isTestnet,
        isLightning,
        activeNetwork,
        activeAccount,
        deviceIdentifiers,
        order,
        false,
        0L
    )
}
