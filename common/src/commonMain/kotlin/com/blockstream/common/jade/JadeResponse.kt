package com.blockstream.common.jade

import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

@Serializable
data class JadeResponse constructor(
    @SerialName("id") val id: String,
    @SerialName("result") val result: JsonElement?,
) : GreenJson<JadeResponse>() {
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