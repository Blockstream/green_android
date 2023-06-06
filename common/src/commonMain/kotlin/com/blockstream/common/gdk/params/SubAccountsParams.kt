package com.blockstream.common.gdk.params

import com.blockstream.common.gdk.GdkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubAccountsParams constructor(
    @SerialName("refresh") val refresh: Boolean? = null,
) : GdkJson<SubAccountsParams>() {
    override val encodeDefaultsValues = false

    override fun kSerializer() = serializer()
}