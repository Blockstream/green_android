package com.blockstream.common.gdk.params

import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubAccountsParams constructor(
    @SerialName("refresh") val refresh: Boolean? = null,
) : GreenJson<SubAccountsParams>() {
    override fun encodeDefaultsValues() = false

    override fun kSerializer() = serializer()
}