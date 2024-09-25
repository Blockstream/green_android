package com.blockstream.common.gdk.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Output constructor(
    @SerialName("address") val address: String? = null,
    @SerialName("domain") val domain: String? = null,
    @SerialName("asset_id") val assetId: String? = null,
    @SerialName("is_change") val isChange: Boolean? = null,
    @SerialName("satoshi") val satoshi: Long,
    // Used in 2FA Redeposit
    @SerialName("user_path") val userPath: List<Long>? = null,
    @SerialName("subtype")val subType: Long? = null,
)