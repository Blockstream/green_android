package com.blockstream.gdk.params

import com.blockstream.gdk.GAJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CredentialsParams constructor(
    @SerialName("password") val password: String? = null
) : GAJson<CredentialsParams>() {
    override val encodeDefaultsValues = false

    override fun kSerializer() = serializer()
}