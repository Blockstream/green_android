package com.blockstream.data.serializers

import com.mohamedrejeb.ksoup.entities.KsoupEntities
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object HtmlEntitiesSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("HtmlEntities", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(KsoupEntities.encodeHtml((value)))
    override fun deserialize(decoder: Decoder): String = KsoupEntities.decodeHtml(decoder.decodeString())
}