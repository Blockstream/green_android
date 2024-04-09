package com.blockstream.common.parcelizer;

import com.arkivanov.essenty.parcelable.CommonParceler
import com.arkivanov.essenty.parcelable.ParcelReader
import com.arkivanov.essenty.parcelable.ParcelWriter
import com.arkivanov.essenty.parcelable.readStringOrNull
import com.arkivanov.essenty.parcelable.writeStringOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement


internal object JsonElementParceler : CommonParceler<JsonElement?> {
    override fun create(reader: ParcelReader): JsonElement? = reader.readStringOrNull()
        ?.let { Json.parseToJsonElement(it) }

    override fun JsonElement?.write(writer: ParcelWriter) {
        writer.writeStringOrNull(this?.let { Json.encodeToString(this) })
    }
}