package com.blockstream.green.database

import androidx.room.*
import androidx.room.ForeignKey.Companion.CASCADE
import com.blockstream.gdk.data.PinData
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
    PIN(0),
    BIOMETRICS(1),
    KEYSTORE(2),
    // It's a variable length PIN, based on greenbits v2
    // in the future can be used as an alphanumeric input as a password
    PASSWORD(3)
}
