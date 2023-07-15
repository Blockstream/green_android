package com.blockstream.common.gdk.data

import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TwoFactorMethodConfig(
    @SerialName("confirmed") val confirmed: Boolean = false,
    @SerialName("enabled") val enabled: Boolean = false,
    @SerialName("data") val data: String = "",
): GreenJson<TwoFactorMethodConfig>() {

    override fun kSerializer() = serializer()
}