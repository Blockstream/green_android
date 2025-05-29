package com.blockstream.common.gdk.data

import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PinData(
    @SerialName("encrypted_data")
    val encryptedData: String,
    @SerialName("pin_identifier")
    val pinIdentifier: String,
    @SerialName("salt")
    val salt: String,
) : GreenJson<PinData>() {

    override fun kSerializer() = serializer()

    companion object {
        fun fromString(jsonString: String): PinData? {
            return try {
                json.decodeFromString(jsonString)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}