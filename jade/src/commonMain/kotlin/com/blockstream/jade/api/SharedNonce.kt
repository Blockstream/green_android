package com.blockstream.jade.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SharedNonceRequestParams(
    val script: ByteArray,
    @SerialName("their_pubkey")
    val theirPubKey: ByteArray
) :
    JadeSerializer<SharedNonceRequestParams>() {
    override fun kSerializer(): KSerializer<SharedNonceRequestParams> = kotlinx.serialization.serializer()
}

@Serializable
data class SharedNonceRequest(
    override val id: String = jadeId(),
    override val method: String = "get_shared_nonce",
    override val params: SharedNonceRequestParams
) : Request<SharedNonceRequest, SharedNonceRequestParams>() {
    override fun kSerializer(): KSerializer<SharedNonceRequest> = kotlinx.serialization.serializer()
}