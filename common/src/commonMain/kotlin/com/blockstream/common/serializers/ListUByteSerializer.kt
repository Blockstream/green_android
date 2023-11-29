package com.blockstream.common.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import saschpe.kase64.base64DecodedBytes
import saschpe.kase64.base64Encoded


@OptIn(ExperimentalUnsignedTypes::class)
object ListUByteSerializer : KSerializer<List<UByte>> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ListUByteSerializer", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: List<UByte>) = encoder.encodeString(value.map { it.toByte() }.toByteArray().base64Encoded)
    override fun deserialize(decoder: Decoder): List<UByte> = decoder.decodeString().base64DecodedBytes.toUByteArray().toList()
}