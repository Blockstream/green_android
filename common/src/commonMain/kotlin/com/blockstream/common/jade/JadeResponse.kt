package com.blockstream.common.jade

import com.arkivanov.essenty.parcelable.CommonParceler
import com.arkivanov.essenty.parcelable.ParcelReader
import com.arkivanov.essenty.parcelable.ParcelWriter
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.arkivanov.essenty.parcelable.TypeParceler
import com.arkivanov.essenty.parcelable.readStringOrNull
import com.arkivanov.essenty.parcelable.writeStringOrNull
import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

internal object JsonElementParceler : CommonParceler<JsonElement?> {
    override fun create(reader: ParcelReader): JsonElement? = reader.readStringOrNull()
        ?.let { Json.parseToJsonElement(it) }

    override fun JsonElement?.write(writer: ParcelWriter) {
        writer.writeStringOrNull(this?.let { Json.encodeToString(this) })
    }
}

@Parcelize
@TypeParceler<JsonElement?, JsonElementParceler>()
@Serializable
data class JadeResponse constructor(
    @SerialName("id") val id: String,
    @SerialName("result") val result: JsonElement?,
) : GreenJson<JadeResponse>(), Parcelable {
    override fun kSerializer() = serializer()

    val httpRequest: JadeHttpRequest? by lazy {
        this.result?.jsonObject?.get("http_request")?.let {
            json.decodeFromJsonElement<JadeHttpRequest>(it)
        }
    }

    val isHttpRequest
        get() = httpRequest != null
}

@Serializable
data class JadeHttpRequest constructor(
    @SerialName("on-reply") val onReply: String,
    @SerialName("params") val params: JsonElement,
) : GreenJson<JadeHttpRequest>() {
    override fun kSerializer() = serializer()

    val isHandshakeInit
        get() = onReply == "handshake_init"

    val isHandshakeComplete
        get() = onReply == "handshake_complete"

    val isPin
        get() = onReply == "pin"
}