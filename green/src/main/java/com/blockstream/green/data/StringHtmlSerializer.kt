package com.blockstream.green.data

import androidx.core.text.TextUtilsCompat
import com.blockstream.green.extensions.fromHtml
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object StringHtmlSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("StringHtml", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(TextUtilsCompat.htmlEncode(value))
    override fun deserialize(decoder: Decoder): String = decoder.decodeString().fromHtml().toString()
}

