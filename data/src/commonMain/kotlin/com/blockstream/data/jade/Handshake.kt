package com.blockstream.data.jade

import com.blockstream.data.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HandshakeInitResponse constructor(
    @SerialName("id")
    val id: String = "0",
    @SerialName("method")
    val method: String = "handshake_init",
    @SerialName("params")
    val params: HandshakeInit,
) : GreenJson<HandshakeInitResponse>() {

    override fun encodeDefaultsValues(): Boolean = true
    override fun kSerializer() = serializer()
}

@Serializable
data class HandshakeInit constructor(
    @SerialName("sig")
    val sig: String,
    @SerialName("ske")
    val ske: String,
) : GreenJson<HandshakeInit>() {

    override fun encodeDefaultsValues(): Boolean = true
    override fun kSerializer() = serializer()
}

@Serializable
data class HandshakeCompleteResponse constructor(
    @SerialName("id")
    val id: String = "C3PO", // From Docs, what can i do?
    @SerialName("method")
    val method: String = "handshake_complete",
    @SerialName("params")
    val params: HandshakeComplete,
) : GreenJson<HandshakeCompleteResponse>() {

    override fun encodeDefaultsValues(): Boolean = true
    override fun kSerializer() = serializer()
}

@Serializable
data class HandshakeComplete constructor(
    @SerialName("encrypted_key")
    val encryptedKey: String,
    @SerialName("hmac")
    val hmac: String,
) : GreenJson<HandshakeComplete>() {

    override fun encodeDefaultsValues(): Boolean = true
    override fun kSerializer() = serializer()
}

