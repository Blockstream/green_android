package com.blockstream.gdk.params

import com.blockstream.gdk.GAJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class InitConfig constructor(
    @SerialName("datadir") val datadir: String,
    @SerialName("log_level") val logLevel: String = "none",
    @SerialName("enable_ss_liquid_hww") val enableSinglesigLiquidHWW: Boolean = false,
) : GAJson<InitConfig>() {

    override fun kSerializer(): KSerializer<InitConfig> {
        return serializer()
    }
}