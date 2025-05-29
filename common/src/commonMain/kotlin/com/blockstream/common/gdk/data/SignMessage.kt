package com.blockstream.common.gdk.data

import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SignMessage(
    @SerialName("signature")
    val signature: String,
) : GreenJson<SignMessage>() {

    override fun kSerializer() = serializer()
}