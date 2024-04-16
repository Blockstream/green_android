package com.blockstream.common.gdk.data

import com.blockstream.common.data.Denomination
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.GreenJson
import com.blockstream.common.utils.toAmountLook
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateTransaction constructor(
    @SerialName("addressees") val addressees: List<Addressee> = listOf(),
    @SerialName("satoshi") val satoshi: Map<String, Long> = mapOf(),
    @SerialName("fee") val fee: Long? = null,
    @SerialName("fee_rate") val feeRate: Long? = null,
    @SerialName("calculated_fee_rate") val calculatedFeeRate: Long? = null,
    @SerialName("old_fee") val oldFee: Long? = null,
    @SerialName("old_fee_rate") val oldFeeRate: Long? = null,
    @SerialName("transaction_outputs") val outputs: List<Output> = listOf(),
    @SerialName("private_key") val privateKey: String? = null,
    @SerialName("memo") val memo: String? = null,
    @SerialName("transaction") val transaction: String? = null,
    @SerialName("error") val error: String? = null,
    @SerialName("txhash") val txHash: String? = null,
    @SerialName("sign_with") var signWith: List<String> = listOf(), // user, green-backendm, all
    @SerialName("is_lightning") val isLightning: Boolean = false, // synthesized
    @SerialName("previous_transaction") val previousTransaction: Transaction? = null, // Bump
) : GreenJson<CreateTransaction>() {
    override fun keepJsonElement() = true

    val isSendAll
        get() = addressees.all { it.isGreedy == true }

    fun isSweep(): Boolean = privateKey?.isNotBlank() ?: false

    fun isSwap(): Boolean = signWith.containsAll(listOf("user", "green-backend")) || signWith.contains("all")

    @Deprecated("Remove it")
    fun utxoViewsDeprecated(showChangeOutputs: Boolean): List<UtxoView> {
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

    suspend fun utxoViews(session: GdkSession, denomination: Denomination?, showChangeOutputs: Boolean): List<UtxoView> {
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

            val amount = it.satoshi.toAmountLook(
                session = session,
                assetId = it.assetId,
                withUnit = true,
                withGrouping = true,
                withMinimumDigits = showChangeOutputs,
                denomination = if (showChangeOutputs) Denomination.BTC else denomination
            )
            val amountExchange = it.satoshi.toAmountLook(
                session = session,
                assetId = it.assetId,
                withUnit = true,
                withGrouping = true,
                denomination = Denomination.fiat(session)
            )


            UtxoView.fromOutput(output = it, amount = amount, amountExchange = amountExchange)
        }
    }

    override fun kSerializer() = serializer()
}
