package com.blockstream.gdk.params

import com.blockstream.gdk.GAJson
import com.blockstream.gdk.data.Credentials
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class EncryptWithPinParams constructor(
    @SerialName("pin") val pin: String,
    @SerialName("plaintext") val plaintext: JsonElement
) : GAJson<EncryptWithPinParams>() {
    override val encodeDefaultsValues = false

    override fun kSerializer() = serializer()

    constructor(pin: String, credentials: Credentials) : this(pin, credentials.toJsonElement())
}