package com.blockstream.common.gdk.params

import com.blockstream.common.gdk.GreenJson
import com.blockstream.common.gdk.data.PinData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DecryptWithPinParams constructor(
    @SerialName("pin") val pin: String? = null,
    @SerialName("pin_data") val pinData: PinData? = null,
) : GreenJson<DecryptWithPinParams>() {
    override fun encodeDefaultsValues() = false

    override fun kSerializer() = serializer()

    companion object{
        fun fromLoginCredentials(loginCredentialsParams: LoginCredentialsParams) : DecryptWithPinParams {
            return DecryptWithPinParams(
                pin = loginCredentialsParams.pin,
                pinData = loginCredentialsParams.pinData,
            )
        }
    }
}