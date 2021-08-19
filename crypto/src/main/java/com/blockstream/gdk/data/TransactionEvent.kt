package com.blockstream.gdk.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransactionEvent(
    @SerialName("subaccounts") val subaccounts: List<Long>,
    @SerialName("txhash") val txHash: String,
    @SerialName("satoshi") val satoshi: Long? = null,
    @SerialName("type") val type: String? = null,
)