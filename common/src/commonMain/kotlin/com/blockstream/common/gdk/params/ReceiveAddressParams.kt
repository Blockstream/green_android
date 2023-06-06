package com.blockstream.common.gdk.params

import com.blockstream.common.gdk.GdkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class ReceiveAddressParams(
    @SerialName("subaccount") val subaccount: Long,
    @SerialName("ignore_gap_limit") val ignoreGapLimit: Boolean = false,
) : GdkJson<ReceiveAddressParams>() {

    override val encodeDefaultsValues: Boolean = false

    override fun kSerializer() = serializer()
}