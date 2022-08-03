package com.blockstream.gdk.params

import com.blockstream.gdk.GAJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class UpdateSubAccountParams constructor(
    @SerialName("subaccount") val subaccount: Long,
    @SerialName("name") val name: String? = null,
    @SerialName("hidden") val hidden: Boolean? = null,
) : GAJson<UpdateSubAccountParams>() {
    override val encodeDefaultsValues: Boolean
        get() = false

    override fun kSerializer(): KSerializer<UpdateSubAccountParams> = serializer()
}