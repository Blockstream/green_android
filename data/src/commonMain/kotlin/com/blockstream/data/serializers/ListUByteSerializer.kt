package com.blockstream.data.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.io.encoding.Base64

@OptIn(ExperimentalUnsignedTypes::class)
object ListUByteSerializer : KSerializer<List<UByte>> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ListUByteSerializer", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: List<UByte>) =
        encoder.encodeString(value.map { it.toByte() }.toByteArray().let { Base64.encode(it) })

    override fun deserialize(decoder: Decoder): List<UByte> = Base64.decode(decoder.decodeString()).toUByteArray().toList()
}