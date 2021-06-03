package com.blockstream.gdk.serializers

import com.blockstream.gdk.data.DeviceSupportsLiquid
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object DeviceSupportsLiquidSerializer : KSerializer<DeviceSupportsLiquid> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("DeviceSupportsLiquid", PrimitiveKind.INT)
    override fun serialize(encoder: Encoder, value: DeviceSupportsLiquid) = encoder.encodeInt(value.ordinal)
    override fun deserialize(decoder: Decoder): DeviceSupportsLiquid = DeviceSupportsLiquid.values()[decoder.decodeInt()]
}

