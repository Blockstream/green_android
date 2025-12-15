package com.blockstream.data.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.io.encoding.Base64

object Base64Serializer : KSerializer<ByteArray?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        "Base64",
        PrimitiveKind.STRING
    )

    override fun serialize(encoder: Encoder, value: ByteArray?) {
        if (value != null) {
            Base64.encode(value)
        } else {
            encoder.encodeByte(0)
        }
    }

    override fun deserialize(decoder: Decoder): ByteArray? {
        try {
            return Base64.decode(decoder.decodeString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}



