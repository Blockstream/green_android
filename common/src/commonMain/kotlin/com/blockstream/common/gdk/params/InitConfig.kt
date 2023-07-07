package com.blockstream.common.gdk.params

import com.blockstream.common.gdk.GdkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InitConfig constructor(
    @SerialName("datadir") val datadir: String,
    @SerialName("log_level") val logLevel: String = "none",
    @SerialName("enable_ss_liquid_hww") val enableSinglesigLiquidHWW: Boolean = true,
) : GdkJson<InitConfig>() {

    override fun kSerializer() = serializer()
}