package com.blockstream.gdk.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransactionEvent(
    @SerialName("subaccounts") val subaccounts: List<Long>,
    // Not used in the App
    // @SerialName("txhash") val txHash: String? = null, // singlesig sends an event without txhash
    @SerialName("satoshi") val satoshi: Long? = null,
    @SerialName("type") val type: String? = null,
)