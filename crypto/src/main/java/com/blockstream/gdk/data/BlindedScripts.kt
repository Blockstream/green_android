package com.blockstream.gdk.data

import com.blockstream.gdk.GAJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BlindedScripts(
    @SerialName("script") val script: String? = null,
    @SerialName("pubkey") val pubkey: String? = null,
): GAJson<BlindedScripts>() {

    override fun kSerializer(): KSerializer<BlindedScripts> {
        return BlindedScripts.serializer()
    }
}
