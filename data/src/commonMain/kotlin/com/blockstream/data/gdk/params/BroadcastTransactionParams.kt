package com.blockstream.data.gdk.params

import com.blockstream.data.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BroadcastTransactionParams constructor(
    @SerialName("transaction")
    val transaction: String? = null,
    @SerialName("psbt")
    val psbt: String? = null,
    @SerialName("memo")
    val memo: String? = null,
    @SerialName("simulate_only")
    val simulateOnly: Boolean = false,
) : GreenJson<BroadcastTransactionParams>() {
    override fun encodeDefaultsValues(): Boolean = false

    override fun kSerializer() = serializer()
}
