package com.blockstream.common.gdk.data

import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.gdk.GdkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class SwapProposal constructor(
    @SerialName("inputs") val inputs: List<SwapAsset>,
    @SerialName("outputs") val outputs: List<SwapAsset>,
    @SerialName("transaction") val transaction: String,
    @SerialName("version") val version: Int,
    @SerialName("proposal") var proposal: String? = null, // This should be injected
) : GdkJson<SwapProposal>(), Parcelable {
    override fun kSerializer() = serializer()
}

@Parcelize
@Serializable
data class SwapAsset constructor(
    @SerialName("amount") val amount: Long,
    @SerialName("asset") val assetId: String,
    @SerialName("amount_blinder") val amountBlinder: String,
    @SerialName("asset_blinder") val assetBlinder: String,
) : GdkJson<SwapAsset>(), Parcelable {
    override fun kSerializer() = serializer()
}