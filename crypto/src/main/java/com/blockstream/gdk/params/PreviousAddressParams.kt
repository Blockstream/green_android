package com.blockstream.gdk.params

import com.blockstream.gdk.GAJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class PreviousAddressParams constructor(
    @SerialName("subaccount") val subaccount: Long,
    @SerialName("last_pointer") val lastPointer: Int? = null,
) : GAJson<PreviousAddressParams>() {

    override val encodeDefaultsValues = false

    override fun kSerializer(): KSerializer<PreviousAddressParams> {
        return serializer()
    }
}
