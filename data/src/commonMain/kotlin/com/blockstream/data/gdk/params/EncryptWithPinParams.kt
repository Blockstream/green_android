package com.blockstream.data.gdk.params

import com.blockstream.data.gdk.GreenJson
import com.blockstream.data.gdk.data.Credentials
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class EncryptWithPinParams constructor(
    @SerialName("pin")
    val pin: String,
    @SerialName("plaintext")
    val plaintext: JsonElement
) : GreenJson<EncryptWithPinParams>() {
    override fun encodeDefaultsValues() = false

    override fun kSerializer() = serializer()

    constructor(pin: String, credentials: Credentials) : this(pin, credentials.toJsonElement())
}