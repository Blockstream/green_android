package com.blockstream.data.gdk.params

import com.blockstream.data.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReconnectHintParams constructor(
    @SerialName("hint")
    val hint: String? = null,
    @SerialName("tor_hint")
    val torHint: String? = null,
) : GreenJson<ReconnectHintParams>() {

    override fun kSerializer() = serializer()

    companion object {
        const val KEY_CONNECT = "connect"
        const val KEY_DISCONNECT = "connect"
    }
}
