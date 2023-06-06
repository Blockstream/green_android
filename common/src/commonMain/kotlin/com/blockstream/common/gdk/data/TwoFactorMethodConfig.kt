package com.blockstream.common.gdk.data

import com.blockstream.common.gdk.GdkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TwoFactorMethodConfig(
    @SerialName("confirmed") val confirmed: Boolean = false,
    @SerialName("enabled") val enabled: Boolean = false,
    @SerialName("data") val data: String = "",
): GdkJson<TwoFactorMethodConfig>() {

    override fun kSerializer() = serializer()
}