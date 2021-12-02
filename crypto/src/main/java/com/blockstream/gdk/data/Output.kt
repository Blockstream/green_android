package com.blockstream.gdk.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Output(
    @SerialName("address") val address: String? = null,
    @SerialName("asset_id") val assetId: String,
    @SerialName("is_change") val isChange: Boolean,
    @SerialName("is_fee") val isFee: Boolean,
    @SerialName("satoshi") val satoshi: Long,
)