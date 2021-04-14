package com.blockstream.gdk.serializers

import com.blockstream.gdk.data.DeviceLiquidSupport
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object DeviceLiquidSupportSerializer : KSerializer<DeviceLiquidSupport> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("DeviceLiquidSupport", PrimitiveKind.INT)
    override fun serialize(encoder: Encoder, value: DeviceLiquidSupport) = encoder.encodeInt(value.ordinal)
    override fun deserialize(decoder: Decoder): DeviceLiquidSupport = DeviceLiquidSupport.values()[decoder.decodeInt()]
}

