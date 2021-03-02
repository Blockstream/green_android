package com.blockstream.gdk.data

import com.blockstream.gdk.GAJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TwoFactorMethodConfig(
    @SerialName("confirmed") val confirmed: Boolean = false,
    @SerialName("enabled") val enabled: Boolean = false,
    @SerialName("data") val data: String = "",
): GAJson<TwoFactorMethodConfig>() {

    override fun kSerializer(): KSerializer<TwoFactorMethodConfig> {
        return TwoFactorMethodConfig.serializer()
    }
}