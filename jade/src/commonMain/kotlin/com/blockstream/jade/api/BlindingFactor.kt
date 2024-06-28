package com.blockstream.jade.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BlindingFactorRequestParams(
    @SerialName("hash_prevouts")
    val hashPrevouts: ByteArray,
    @SerialName("output_index")
    val outputIdx: Int,
    val type: String
) :
    JadeSerializer<BlindingFactorRequestParams>() {
    override fun kSerializer(): KSerializer<BlindingFactorRequestParams> = kotlinx.serialization.serializer()
}

@Serializable
data class BlindingFactorRequest(
    override val id: String = jadeId(),
    override val method: String = "get_blinding_factor",
    override val params: BlindingFactorRequestParams
) : Request<BlindingFactorRequest, BlindingFactorRequestParams>() {
    override fun kSerializer(): KSerializer<BlindingFactorRequest> = kotlinx.serialization.serializer()
}