package com.blockstream.data.gdk.params

import com.blockstream.data.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CredentialsParams constructor(
    @SerialName("password")
    val password: String? = null
) : GreenJson<CredentialsParams>() {
    override fun encodeDefaultsValues() = false

    override fun kSerializer() = serializer()
}