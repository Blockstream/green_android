package com.blockstream.common.gdk.data

import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InputUnblindedData(
    @SerialName("vin") val vin: UInt,
    @SerialName("asset_id") val assetId: String,
    @SerialName("satoshi") val satoshi: Long,
    @SerialName("assetblinder") val assetblinder: String,
    @SerialName("amountblinder") val amountblinder: String,
) : GreenJson<InputUnblindedData>() {
    override fun kSerializer() = serializer()
}