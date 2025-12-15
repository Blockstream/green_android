package com.blockstream.green.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.TypeConverters
import com.blockstream.data.data.CredentialType
import com.blockstream.data.data.EncryptedData
import com.blockstream.data.gdk.data.PinData

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
    var counter: Long = 0,
)
