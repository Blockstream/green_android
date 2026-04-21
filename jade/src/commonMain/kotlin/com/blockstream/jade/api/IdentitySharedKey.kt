package com.blockstream.jade.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IdentitySharedKeyRequestParams(
    val identity: String,
    val curve: String,
    @SerialName("their_pubkey")
    val theirPubKey: ByteArray,
    val index: Long? = null,
) : JadeSerializer<IdentitySharedKeyRequestParams>() {
    override fun kSerializer(): KSerializer<IdentitySharedKeyRequestParams> = kotlinx.serialization.serializer()
}

@Serializable
data class IdentitySharedKeyRequest(
    override val id: String = jadeId(),
    override val method: String = "get_identity_shared_key",
    override val params: IdentitySharedKeyRequestParams,
) : Request<IdentitySharedKeyRequest, IdentitySharedKeyRequestParams>() {
    override fun kSerializer(): KSerializer<IdentitySharedKeyRequest> = kotlinx.serialization.serializer()
}
