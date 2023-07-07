package com.blockstream.common.gdk.params

import com.blockstream.common.gdk.GdkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CredentialsParams constructor(
    @SerialName("password") val password: String? = null
) : GdkJson<CredentialsParams>() {
    override fun encodeDefaultsValues() = false

    override fun kSerializer() = serializer()
}