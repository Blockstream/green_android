package com.blockstream.common.gdk.data

import com.blockstream.common.gdk.GdkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateTransaction constructor(
    @SerialName("addressees") val addressees: List<Addressee> = listOf(),
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
    @SerialName("sign_with") var signWith: List<String> = listOf(),
    @SerialName("is_lightning") val isLightning: Boolean = false, // synthesized
) : GdkJson<CreateTransaction>() {
    override val keepJsonElement = true

    fun utxoViews(showChangeOutputs: Boolean): List<UtxoView> {
        val outputs = outputs.filter {
            !it.address.isNullOrBlank()
        }

        return outputs.filter {
            if (showChangeOutputs || isSweep) {
                true
            } else {
                !it.isChange
            }
        }.map {
            UtxoView.fromOutput(it)
        }
    }

    override fun kSerializer() = serializer()
}
