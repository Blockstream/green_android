package com.blockstream.jade.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

@Serializable
data class BlindingKeyRequestParams(
    val script: ByteArray,
) :
    JadeSerializer<BlindingKeyRequestParams>() {
    override fun kSerializer(): KSerializer<BlindingKeyRequestParams> = kotlinx.serialization.serializer()
}

@Serializable
data class BlindingKeyRequest(
    override val id: String = jadeId(),
    override val method: String = "get_blinding_key",
    override val params: BlindingKeyRequestParams
) : Request<BlindingKeyRequest, BlindingKeyRequestParams>() {
    override fun kSerializer(): KSerializer<BlindingKeyRequest> = kotlinx.serialization.serializer()
}