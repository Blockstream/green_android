package com.blockstream.common.gdk.params

import com.blockstream.common.gdk.GdkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SignMessageParams constructor(
    @SerialName("address") val address: String,
    @SerialName("message") var message: String,
) : GdkJson<SignMessageParams>() {
    override fun kSerializer() = serializer()
}