package com.blockstream.common.gdk.data

import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TwoFactorReset constructor(
    @SerialName("is_active")
    val isActive: Boolean? = null,
    @SerialName("days_remaining")
    val daysRemaining: Int = 0,
    @SerialName("is_disputed")
    val isDisputed: Boolean? = null
) : GreenJson<TwoFactorReset>() {
    override fun kSerializer() = serializer()
}
