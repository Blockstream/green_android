package com.blockstream.common.gdk.data

import com.blockstream.common.gdk.GdkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SignMessage(
    @SerialName("signature") val signature: String,
): GdkJson<SignMessage>(){

    override fun kSerializer() = serializer()
}