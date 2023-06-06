package com.blockstream.green.database

import androidx.room.*
import androidx.room.ForeignKey.Companion.CASCADE
import com.blockstream.common.gdk.data.PinData
import com.blockstream.green.utils.EncryptedData


@Entity(
    tableName = "login_credentials",
    primaryKeys = ["wallet_id", "credential_type"],
    foreignKeys = [ForeignKey(
        entity = Wallet::class,
        parentColumns = ["id"],
        childColumns = ["wallet_id"],
        onDelete = CASCADE
    )]
)
@TypeConverters(Converters::class)
data class LoginCredentials constructor(
    @ColumnInfo(name = "wallet_id")
    var walletId: Long,

    @ColumnInfo(name = "network", defaultValue = "")
    var network: String,

    @ColumnInfo(name = "credential_type")
    var credentialType: CredentialType,

    @ColumnInfo(name = "pin_data")
    var pinData: PinData? = null,

    @ColumnInfo(name = "keystore")
    var keystore: String? = null,

    @ColumnInfo(name = "encrypted_data")
    var encryptedData: EncryptedData? = null,

    @ColumnInfo(name = "counter")
    var counter: Int = 0,
)


enum class CredentialType(val value: Int) {
    PIN_PINDATA(0),
    BIOMETRICS_PINDATA(1),
    KEYSTORE_PASSWORD(2), // Deprecated, use WatchOnlyCredentials
    PASSWORD_PINDATA(3), // It's a variable length PIN (string), based on greenbits v2
    KEYSTORE_WATCHONLY_CREDENTIALS(4),
    BIOMETRICS_WATCHONLY_CREDENTIALS(5),
    KEYSTORE_GREENLIGHT_CREDENTIALS(6),
}
