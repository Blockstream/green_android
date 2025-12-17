package com.blockstream.data.gdk.data

import com.blockstream.data.data.Denomination
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.GreenJson
import com.blockstream.data.utils.toAmountLook
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateTransaction constructor(
    @SerialName("addressees")
    val addressees: List<Addressee> = listOf(),
    @SerialName("satoshi")
    val satoshi: Map<String, Long> = mapOf(),
    @SerialName("fee")
    val fee: Long? = null,
    @SerialName("fee_rate")
    val feeRate: Long? = null,
    @SerialName("calculated_fee_rate")
    val calculatedFeeRate: Long? = null,
    @SerialName("old_fee")
    val oldFee: Long? = null,
    @SerialName("old_fee_rate")
    val oldFeeRate: Long? = null,
    @SerialName("transaction_outputs")
    val outputs: List<Output> = listOf(),
    @SerialName("private_key")
    val privateKey: String? = null,
    @SerialName("memo")
    val memo: String? = null,
    @SerialName("transaction")
    val transaction: String? = null,
    @SerialName("error")
    val error: String? = null,
    @SerialName("txhash")
    val txHash: String? = null,
    @SerialName("sign_with")
    var signWith: List<String> = listOf(), // user, green-backend, all
    @SerialName("is_lightning")
    val isLightning: Boolean = false, // synthesized
    @SerialName("is_lightning_description_editable")
    val isLightningDescriptionEditable: Boolean = false, // synthesized
    @SerialName("previous_transaction")
    val previousTransaction: Transaction? = null, // Bump
) : GreenJson<CreateTransaction>() {
    override fun keepJsonElement() = true

    val isSendAll
        get() = addressees.all { it.isGreedy == true }

    fun isBump(): Boolean = previousTransaction != null

    fun isSweep(): Boolean = privateKey?.isNotBlank() ?: false

    fun isAtomicSwap(): Boolean = signWith.containsAll(listOf("user", "green-backend")) || signWith.contains("all")

    suspend fun utxoViews(
        session: GdkSession,
        denomination: Denomination?,
        isAddressVerificationOnDevice: Boolean,
        showChangeOutputs: Boolean
    ): List<UtxoView> {
        val outputs = outputs.filter {
            !it.address.isNullOrBlank()
        }

        return outputs.filter {
            if (showChangeOutputs) {
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
                withMinimumDigits = isAddressVerificationOnDevice,
                denomination = if (isAddressVerificationOnDevice) Denomination.BTC else denomination
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
