package com.blockstream.common.gdk.data

import com.blockstream.common.data.Denomination
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.GreenJson
import com.blockstream.common.utils.toAmountLook
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BroadcastTransaction constructor(
    @SerialName("txhash") val txhash: String? = null,
    @SerialName("psbt") val psbt: String? = null,
) : GreenJson<BroadcastTransaction>() {
    override fun keepJsonElement() = true

    override fun kSerializer() = serializer()
}
