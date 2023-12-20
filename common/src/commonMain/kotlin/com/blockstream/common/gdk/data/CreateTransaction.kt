package com.blockstream.common.gdk.data

import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateTransaction constructor(
    @SerialName("addressees") val addressees: List<Addressee> = listOf(),
    @SerialName("satoshi") val satoshi: Map<String, Long> = mapOf(),
    @SerialName("fee") val fee: Long? = null,
    @SerialName("fee_rate") val feeRate: Long? = null,
    @SerialName("transaction_outputs") val outputs: List<Output> = listOf(),
    @SerialName("private_key") val privateKey: String? = null,
    @SerialName("memo") val memo: String? = null,
    @SerialName("transaction") val transaction: String? = null,
    @SerialName("error") val error: String? = null,
    @SerialName("txhash") val txHash: String? = null,
    @SerialName("sign_with") var signWith: List<String> = listOf(), // user, green-backendm, all
    @SerialName("is_lightning") val isLightning: Boolean = false, // synthesized
) : GreenJson<CreateTransaction>() {
    override fun keepJsonElement() = true

    val isSendAll
        get() = addressees.all { it.isGreedy == true }

    fun isSweep(): Boolean = privateKey?.isNotBlank() ?: false

    fun isSwap(): Boolean = signWith.containsAll(listOf("user", "green-backend")) || signWith.contains("all")

    fun utxoViews(showChangeOutputs: Boolean): List<UtxoView> {
        val outputs = outputs.filter {
            !it.address.isNullOrBlank()
        }

        return outputs.filter {
            if (showChangeOutputs || isSweep()) {
                true
            } else {
                it.isChange != true
            }
        }.map {
            UtxoView.fromOutput(it)
        }
    }

    override fun kSerializer() = serializer()
}
