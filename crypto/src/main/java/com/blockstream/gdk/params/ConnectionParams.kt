package com.blockstream.gdk.params

import com.blockstream.gdk.GAJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class ConnectionParams(
    @SerialName("name") val networkName: String,
    @SerialName("use_tor") val useTor: Boolean,
    @SerialName("log_level") val logLevel: String = "none",
    @SerialName("user_agent") val userAgent: String,
    @SerialName("proxy") val proxy: String,
    @SerialName("spv_enabled") val svpEnabled: Boolean = false,
) : GAJson<ConnectionParams>() {

    override fun kSerializer(): KSerializer<ConnectionParams> {
        return serializer()
    }
}
