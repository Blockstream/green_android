package com.blockstream.gdk.serializers

import com.blockstream.gdk.data.DeviceSupportsAntiExfilProtocol
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object DeviceSupportsAntiExfilProtocolSerializer : KSerializer<DeviceSupportsAntiExfilProtocol> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("DeviceSupportsAntiExfilProtocol", PrimitiveKind.INT)
    override fun serialize(encoder: Encoder, value: DeviceSupportsAntiExfilProtocol) = encoder.encodeInt(value.ordinal)
    override fun deserialize(decoder: Decoder): DeviceSupportsAntiExfilProtocol = DeviceSupportsAntiExfilProtocol.values()[decoder.decodeInt()]
}

