package com.blockstream.common.gdk.params

import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InitConfig constructor(
    @SerialName("datadir") val datadir: String,
    @SerialName("log_level") val logLevel: String = "none",
    @SerialName("optimize_expired_csv") val optimizeExpiredCsv: Boolean = true
) : GreenJson<InitConfig>() {

    override fun kSerializer() = serializer()
}