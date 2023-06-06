package com.blockstream.common.gdk.params

import com.blockstream.common.gdk.GdkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReconnectHintParams constructor(
    @SerialName("hint") val hint: String? = null,
    @SerialName("tor_hint") val torHint: String? = null,
) : GdkJson<ReconnectHintParams>() {

    override fun kSerializer() = serializer()

    companion object {
        const val KEY_CONNECT = "connect"
        const val KEY_DISCONNECT = "connect"
    }
}
