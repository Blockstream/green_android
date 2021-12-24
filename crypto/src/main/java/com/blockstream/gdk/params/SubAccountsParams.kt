package com.blockstream.gdk.params

import com.blockstream.gdk.GAJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubAccountsParams constructor(
    @SerialName("refresh") val refresh: Boolean? = null,
) : GAJson<SubAccountsParams>() {
    override val encodeDefaultsValues = false

    override fun kSerializer() = serializer()
}