package com.blockstream.common.serializers

import com.blockstream.common.data.toDeviceIdentifierList
import com.blockstream.common.data.toJson
import com.blockstream.common.data.toWalletExtras
import com.blockstream.common.database.wallet.Wallet
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@OptIn(ExperimentalSerializationApi::class)
object WalletSerializer : KSerializer<Wallet> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Wallet", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): Wallet {
        return Wallet(
            decoder.decodeString(), // id
            decoder.decodeString(), // name
            decoder.decodeString(), // xpub_hash_id
            decoder.decodeString(), // active_network
            decoder.decodeLong(), // active_account
            decoder.decodeBoolean(), // is_testnet
            decoder.decodeBoolean(), // is_hardware
            decoder.decodeBoolean(), // is_lightning
            decoder.decodeBoolean(), // ask_bip39_passphrase
            decoder.decodeNullableSerializableValue(String.serializer()), // watch_only_username
            decoder.decodeNullableSerializableValue(String.serializer())?.toDeviceIdentifierList(), // device_identifiers
            decoder.decodeNullableSerializableValue(String.serializer())?.toWalletExtras(), // extras
            decoder.decodeLong() // order
        )
    }

    override fun serialize(encoder: Encoder, value: Wallet) {
        encoder.encodeString(value.id)
        encoder.encodeString(value.name)
        encoder.encodeString(value.xpub_hash_id)
        encoder.encodeString(value.active_network)
        encoder.encodeLong(value.active_account)
        encoder.encodeBoolean(value.is_testnet)
        encoder.encodeBoolean(value.is_hardware)
        encoder.encodeBoolean(value.is_lightning)
        encoder.encodeBoolean(value.ask_bip39_passphrase)
        encoder.encodeNullableSerializableValue(String.serializer(), value.watch_only_username)
        encoder.encodeNullableSerializableValue(String.serializer(), value.device_identifiers?.toJson())
        encoder.encodeNullableSerializableValue(String.serializer(), value.extras?.toJson())
        encoder.encodeLong(value.order)
    }

}

