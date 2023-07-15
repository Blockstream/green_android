package com.blockstream.common.gdk.data

import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BlindedScripts(
    @SerialName("script") val script: String? = null,
    @SerialName("pubkey") val pubkey: String? = null,
): GreenJson<BlindedScripts>() {

    override fun kSerializer() = serializer()
}
