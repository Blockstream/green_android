package com.blockstream.gdk.params

import com.blockstream.gdk.GAJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class ReceiveAddressParams(
    @SerialName("subaccount") val subaccount: Long,
//    @SerialName("address_type") val addressType: String = "p2wsh",
) : GAJson<ReceiveAddressParams>() {

    override fun kSerializer(): KSerializer<ReceiveAddressParams> {
        return serializer()
    }
}