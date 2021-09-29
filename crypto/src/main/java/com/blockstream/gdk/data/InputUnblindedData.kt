package com.blockstream.gdk.data

import com.blockstream.gdk.GAJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InputUnblindedData(
    @SerialName("vin") val vin: Long,
    @SerialName("asset_id") val assetId: String,
    @SerialName("satoshi") val satoshi: Long,
    @SerialName("assetblinder") val assetblinder: String,
    @SerialName("amountblinder") val amountblinder: String,
) : GAJson<InputUnblindedData>() {
    override fun kSerializer(): KSerializer<InputUnblindedData> = serializer()
}