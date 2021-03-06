package com.blockstream.gdk.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Notification(
    @SerialName("event") val event: String,
    @SerialName("block") val block: Block? = null,
    @SerialName("fees") val fees: List<Long>? = null,
    @SerialName("twofactor_reset") val twoFactorReset: TwoFactorReset? = null,
    @SerialName("settings") val settings: Settings? = null,
    @SerialName("tor") val torStatus: TORStatus? = null,
    @SerialName("network") val network: NetworkEvent? = null,
)