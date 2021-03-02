package com.blockstream.gdk.params

import com.blockstream.gdk.GAJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class InitConfig(
    @SerialName("datadir") val datadir: String,
) : GAJson<InitConfig>() {

    override fun kSerializer(): KSerializer<InitConfig> {
        return serializer()
    }
}