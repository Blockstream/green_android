package com.blockstream.gdk.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Output(
    @SerialName("address") val address: String? = null,
    @SerialName("asset_id") val assetId: String? = null,
    @SerialName("is_change") val isChange: Boolean,
    @SerialName("satoshi") val satoshi: Long,
)