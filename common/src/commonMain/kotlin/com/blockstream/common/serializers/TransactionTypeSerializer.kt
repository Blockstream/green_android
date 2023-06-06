package com.blockstream.common.serializers

import com.blockstream.common.gdk.data.Transaction
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object TransactionTypeSerializer : KSerializer<Transaction.Type> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("TransactionType", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Transaction.Type) = encoder.encodeString(value.gdkType)
    override fun deserialize(decoder: Decoder): Transaction.Type = Transaction.Type.from(decoder.decodeString())
}

