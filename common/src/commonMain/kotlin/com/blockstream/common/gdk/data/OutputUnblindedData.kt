package com.blockstream.common.gdk.data

import com.blockstream.common.gdk.GdkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OutputUnblindedData(
    @SerialName("vout") val vout: UInt,
    @SerialName("asset_id") val assetId: String,
    @SerialName("satoshi") val satoshi: Long,
    @SerialName("assetblinder") val assetblinder: String,
    @SerialName("amountblinder") val amountblinder: String,
) : GdkJson<OutputUnblindedData>() {
    override fun kSerializer() = serializer()
}