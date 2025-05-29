package com.blockstream.common.gdk.data

import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Psbt constructor(
    @SerialName("blinding_nonces")
    val blindingNonces: List<String>? = null,
    @SerialName("psbt")
    val psbt: String, // in base64
) : GreenJson<Psbt>() {
    override fun keepJsonElement() = true
    override fun kSerializer() = serializer()
}
