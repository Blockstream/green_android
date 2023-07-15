package com.blockstream.common.database

import app.cash.sqldelight.ColumnAdapter
import com.blockstream.common.data.CredentialType
import com.blockstream.common.data.DeviceIdentifier
import com.blockstream.common.data.EncryptedData
import com.blockstream.common.data.WalletExtras
import com.blockstream.common.data.toDeviceIdentifierList
import com.blockstream.common.data.toJson
import com.blockstream.common.gdk.data.PinData

val deviceIdentifierAdapter = object : ColumnAdapter<List<DeviceIdentifier>, String> {
    override fun decode(databaseValue: String) = if (databaseValue.isEmpty()) {
        listOf()
    } else {
        databaseValue.toDeviceIdentifierList() ?: emptyList()
    }

    override fun encode(value: List<DeviceIdentifier>): String = value.toJson()
}

val walletExtrasTypeAdapter = object : ColumnAdapter<WalletExtras, String> {
    override fun decode(databaseValue: String) = WalletExtras.fromString(databaseValue)
    override fun encode(value: WalletExtras): String = value.toJson()
}

val credentialsTypeAdapter = object : ColumnAdapter<CredentialType, Long> {
    override fun decode(databaseValue: Long) = CredentialType.byPosition(databaseValue)
    override fun encode(value: CredentialType): Long = value.value
}

val pinDataAdapter = object : ColumnAdapter<PinData, String> {
    override fun decode(databaseValue: String) = PinData.fromString(databaseValue)!!
    override fun encode(value: PinData): String = value.toJson()
}

val encryptedDataAdapter = object : ColumnAdapter<EncryptedData, String> {
    override fun decode(databaseValue: String) = EncryptedData.fromString(databaseValue)!!
    override fun encode(value: EncryptedData): String = value.toJson()
}