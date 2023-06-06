package com.blockstream.common.gdk.data

import com.blockstream.common.gdk.GdkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BlindedScripts(
    @SerialName("script") val script: String? = null,
    @SerialName("pubkey") val pubkey: String? = null,
): GdkJson<BlindedScripts>() {

    override fun kSerializer() = serializer()
}
