package com.blockstream.gdk.serializers

import com.blockstream.gdk.data.AccountType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object AccountTypeSerializer : KSerializer<AccountType> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("AccountType", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: AccountType) = encoder.encodeString(value.gdkType)
    override fun deserialize(decoder: Decoder): AccountType = AccountType.byGDKType(decoder.decodeString())
}

