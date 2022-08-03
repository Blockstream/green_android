package com.blockstream.gdk.data

import android.os.Parcelable
import com.blockstream.gdk.GAJson
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.KSerializer
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
) : GAJson<SwapProposal>(), Parcelable {
    override fun kSerializer(): KSerializer<SwapProposal> = serializer()
}

@Parcelize
@Serializable
data class SwapAsset constructor(
    @SerialName("amount") val amount: Long,
    @SerialName("asset") val assetId: String,
    @SerialName("amount_blinder") val amountBlinder: String,
    @SerialName("asset_blinder") val assetBlinder: String,
) : GAJson<SwapAsset>(), Parcelable {
    override fun kSerializer(): KSerializer<SwapAsset> = serializer()
}