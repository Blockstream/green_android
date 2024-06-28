package com.blockstream.jade.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class HandshakeInitRequestParams(
    @SerialName("sig") val sig: String,
    @SerialName("ske") val ske: String,
) : JadeSerializer<HandshakeInitRequestParams>() {
    override fun kSerializer(): KSerializer<HandshakeInitRequestParams> = kotlinx.serialization.serializer()
}

@Serializable
data class HandshakeInitRequest(
    override val id: String = jadeId(),
    override val method: String = "handshake_init",
    override val params: HandshakeInitRequestParams
) : Request<HandshakeInitRequest, HandshakeInitRequestParams>() {
    override fun kSerializer(): KSerializer<HandshakeInitRequest> = kotlinx.serialization.serializer()
}

@Serializable
data class HandshakeCompleteRequestParams(
    @SerialName("encrypted_key") val encryptedKey: String,
    @SerialName("hmac") val hmac: String,
) : JadeSerializer<HandshakeCompleteRequestParams>() {
    override fun kSerializer(): KSerializer<HandshakeCompleteRequestParams> = kotlinx.serialization.serializer()
}

@Serializable
data class HandshakeCompleteRequest(
    override val id: String = jadeId(),
    override val method: String = "handshake_complete",
    override val params: HandshakeCompleteRequestParams
) : Request<HandshakeCompleteRequest, HandshakeCompleteRequestParams>() {
    override fun kSerializer(): KSerializer<HandshakeCompleteRequest> = kotlinx.serialization.serializer()
}
