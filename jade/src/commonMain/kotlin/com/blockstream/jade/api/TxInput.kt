package com.blockstream.jade.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TxInput(
    @SerialName("is_witness")
    val isWitness: Boolean,
    @SerialName("input_tx")
    val inputTx: ByteArray? = null,
    val script: ByteArray? = null,
    val satoshi: Long? = null,
    @SerialName("value_commitment")
    val valueCommitment: ByteArray? = null,
    val path: List<Long>? = null,
    @SerialName("ae_host_commitment")
    val aeHostCommitment: ByteArray? = null,
    @SerialName("ae_host_entropy")
    val aeHostEntropy: ByteArray? = null
) : JadeSerializer<TxInput>() {
    override fun kSerializer() = serializer()
    override fun encodeDefaultsValues() = false
}

@Serializable
data class TxInputRequest(
    override val id: String,
    override val method: String,
    override val params: TxInput
) : Request<TxInputRequest, TxInput>() {
    override fun encodeDefaultsValues() = false

    override fun kSerializer() = serializer()
}