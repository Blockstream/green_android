package com.blockstream.common.gdk.params

import com.blockstream.common.gdk.GdkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class UnspentOutputsPrivateKeyParams constructor(
    @SerialName("private_key") val privateKey: String,
    @SerialName("password") val password: String? = null
) : GdkJson<UnspentOutputsPrivateKeyParams>() {
    override fun encodeDefaultsValues() = false

    override fun kSerializer() = serializer()
}
