package com.blockstream.gdk.params

import com.blockstream.gdk.GAJson
import com.blockstream.gdk.data.PinData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mu.KLogging

@Serializable
data class DecryptWithPinParams constructor(
    @SerialName("pin") val pin: String? = null,
    @SerialName("pin_data") val pinData: PinData? = null,
) : GAJson<DecryptWithPinParams>() {
    override val encodeDefaultsValues = false

    override fun kSerializer() = serializer()

    companion object: KLogging(){
        fun fromLoginCredentials(loginCredentialsParams: LoginCredentialsParams) : DecryptWithPinParams{
            return DecryptWithPinParams(
                pin = loginCredentialsParams.pin,
                pinData = loginCredentialsParams.pinData,
            )
        }
    }
}