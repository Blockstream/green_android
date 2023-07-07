package com.blockstream.common.gdk.data

import com.blockstream.common.gdk.GdkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InputUnblindedData(
    @SerialName("vin") val vin: UInt,
    @SerialName("asset_id") val assetId: String,
    @SerialName("satoshi") val satoshi: Long,
    @SerialName("assetblinder") val assetblinder: String,
    @SerialName("amountblinder") val amountblinder: String,
) : GdkJson<InputUnblindedData>() {
    override fun kSerializer() = serializer()
}