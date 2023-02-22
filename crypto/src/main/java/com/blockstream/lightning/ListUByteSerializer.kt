package com.blockstream.lightning

import android.util.Base64
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder



object ListUByteSerializer : KSerializer<List<UByte>> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ListUByteSerializer", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: List<UByte>) = encoder.encodeString(Base64.encodeToString(value.map { it.toByte() }.toByteArray(), Base64.NO_WRAP))
    override fun deserialize(decoder: Decoder): List<UByte> = decoder.decodeString().let {
        Base64.decode(it, Base64.NO_WRAP).toUByteArray().toList()
    }
}