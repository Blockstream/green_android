package com.blockstream.data.gdk.params

import com.blockstream.data.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubAccountsParams constructor(
    @SerialName("refresh")
    val refresh: Boolean? = null,
) : GreenJson<SubAccountsParams>() {
    override fun encodeDefaultsValues() = false

    override fun kSerializer() = serializer()
}