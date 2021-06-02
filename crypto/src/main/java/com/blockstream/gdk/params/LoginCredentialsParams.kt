package com.blockstream.gdk.params

import com.blockstream.gdk.GAJson
import com.blockstream.gdk.data.Device
import com.blockstream.gdk.data.PinData
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class LoginCredentialsParams(
    @SerialName("mnemonic") val mnemonic: String? = null,
    @SerialName("password") val password: String? = null,
    @SerialName("pin") val pin: String? = null,
    @SerialName("pin_data") val pinData: PinData? = null,
    @SerialName("username") val username: String? = null
) : GAJson<LoginCredentialsParams>() {
    override val encodeDefaultsValues = false

    override fun kSerializer(): KSerializer<LoginCredentialsParams> {
        return serializer()
    }
}