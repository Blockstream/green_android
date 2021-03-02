package com.blockstream.gdk.data

import com.blockstream.gdk.GAJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PinData(
    @SerialName("encrypted_data") val encryptedData: String,
    @SerialName("pin_identifier") val pinIdentifier: String,
    @SerialName("salt") val salt: String,
): GAJson<PinData>() {

    override fun kSerializer(): KSerializer<PinData> {
        return serializer()
    }
}