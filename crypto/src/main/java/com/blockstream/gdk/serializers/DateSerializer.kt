package com.blockstream.gdk.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.text.SimpleDateFormat
import java.util.*

object DateOrNullSerializer : KSerializer<Date?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Date", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Date?) = encoder.encodeString(value?.let {
        SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale.getDefault()
        ).format(value)
    } ?: "")

    override fun deserialize(decoder: Decoder): Date? =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(decoder.decodeString())
}

object DateSerializer : KSerializer<Date> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Date", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Date) = encoder.encodeString(
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(value)
    )

    override fun deserialize(decoder: Decoder): Date =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(decoder.decodeString())
            ?: Date(0)
}

