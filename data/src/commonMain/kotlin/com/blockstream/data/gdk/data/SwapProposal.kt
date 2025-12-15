package com.blockstream.data.gdk.data

import com.blockstream.data.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SwapProposal constructor(
    @SerialName("inputs")
    val inputs: List<SwapAsset>,
    @SerialName("outputs")
    val outputs: List<SwapAsset>,
    @SerialName("transaction")
    val transaction: String,
    @SerialName("version")
    val version: Int,
    @SerialName("proposal")
    var proposal: String? = null, // This should be injected
) : GreenJson<SwapProposal>() {
    override fun kSerializer() = serializer()
}

@Serializable
data class SwapAsset constructor(
    @SerialName("amount")
    val amount: Long,
    @SerialName("asset")
    val assetId: String,
    @SerialName("amount_blinder")
    val amountBlinder: String,
    @SerialName("asset_blinder")
    val assetBlinder: String,
) : GreenJson<SwapAsset>() {
    override fun kSerializer() = serializer()
}