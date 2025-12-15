package com.blockstream.data.gdk.params

import com.blockstream.data.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SignMessageParams constructor(
    @SerialName("address")
    val address: String,
    @SerialName("message")
    var message: String,
) : GreenJson<SignMessageParams>() {
    override fun kSerializer() = serializer()
}