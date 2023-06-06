package com.blockstream.common.gdk.data

import com.blockstream.common.gdk.GdkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PinData(
    @SerialName("encrypted_data") val encryptedData: String,
    @SerialName("pin_identifier") val pinIdentifier: String,
    @SerialName("salt") val salt: String,
): GdkJson<PinData>() {

    override fun kSerializer() = serializer()
}