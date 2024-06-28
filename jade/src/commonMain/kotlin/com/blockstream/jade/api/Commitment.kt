package com.blockstream.jade.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Commitment(
    @SerialName("asset_id")
    val assetId: ByteArray,
    val value: Long?,
    val abf: ByteArray?,
    val vbf: ByteArray?,
    @SerialName("blinding_key")
    val blindingKey: ByteArray
) : JadeSerializer<Commitment>() {
    override fun kSerializer() = serializer()
    override fun encodeDefaultsValues() = false
}
