package com.blockstream.green.data.meld.data

import kotlinx.serialization.Serializable

@Serializable
data class CryptoWidget(
    val id: String,
    val externalSessionId: String? = null,
    val externalCustomerId: String? = null,
    val customerId: String,
    val widgetUrl: String,
    val token: String
)