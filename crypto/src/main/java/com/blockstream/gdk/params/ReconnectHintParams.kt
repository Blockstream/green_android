package com.blockstream.gdk.params

import com.blockstream.gdk.GAJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReconnectHintParams(
    @SerialName("hint") val hint: String = "now",
) : GAJson<ReconnectHintParams>() {

    override fun kSerializer(): KSerializer<ReconnectHintParams> {
        return serializer()
    }
}
