package com.blockstream.gdk.data

import com.blockstream.gdk.GAJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateTransaction constructor(
    @SerialName("addressees") val addressees: List<Addressee>,
    @SerialName("satoshi") val satoshi: Map<String, Long> = mapOf(),
    @SerialName("fee") val fee: Long? = null,
    @SerialName("fee_rate") val feeRate: Long? = null,
    @SerialName("addressees_read_only") val isReadOnly: Boolean = false,
    @SerialName("send_all") val isSendAll: Boolean = false,
    @SerialName("transaction_outputs") val outputs: List<Output> = listOf(),
    @SerialName("is_sweep") val isSweep: Boolean = false,
    @SerialName("memo") val memo: String? = null,
    @SerialName("transaction") val transaction: String? = null,
    @SerialName("error") val error: String? = null,
    @SerialName("txhash") val txHash: String? = null,
) : GAJson<CreateTransaction>() {
    override val keepJsonElement = true

    override fun kSerializer(): KSerializer<CreateTransaction> = serializer()
}
