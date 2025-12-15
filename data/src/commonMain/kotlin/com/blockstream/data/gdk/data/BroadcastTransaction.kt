package com.blockstream.data.gdk.data

import com.blockstream.data.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BroadcastTransaction constructor(
    @SerialName("txhash")
    val txhash: String? = null,
    @SerialName("psbt")
    val psbt: String? = null,
) : GreenJson<BroadcastTransaction>() {
    override fun keepJsonElement() = true

    override fun kSerializer() = serializer()
}
