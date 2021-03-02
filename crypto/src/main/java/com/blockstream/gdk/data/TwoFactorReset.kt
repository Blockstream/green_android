package com.blockstream.gdk.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TwoFactorReset(
    @SerialName("days_remaining") val daysRemaining: Int,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("is_disputed") val isDisputed: Boolean
)
