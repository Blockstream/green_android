package com.blockstream.green.database

import androidx.room.*
import androidx.room.ForeignKey.CASCADE
import com.blockstream.green.utils.EncryptedData
import com.blockstream.gdk.GreenWallet
import com.blockstream.gdk.data.PinData
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString


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
data class LoginCredentials(
    @ColumnInfo(name = "wallet_id")
    var walletId: Long,

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

class Converters {
    @TypeConverter
    fun toType(value: Int) = enumValues<CredentialType>()[value]

    @TypeConverter
    fun fromType(value: CredentialType) = value.ordinal

    @TypeConverter
    fun toPinData(value: String?): PinData? = value?.let {
        GreenWallet.JsonDeserializer.decodeFromString(it)
    }

    @TypeConverter
    fun fromPinData(value: PinData?): String? = value?.let { GreenWallet.JsonDeserializer.encodeToString(it)}

    @TypeConverter
    fun toEncryptedData(value: String?): EncryptedData? = value?.let {
        GreenWallet.JsonDeserializer.decodeFromString(it)
    }

    @TypeConverter
    fun fromEncryptedData(value: EncryptedData?): String? = value?.let {
        GreenWallet.JsonDeserializer.encodeToString(it)}
}
