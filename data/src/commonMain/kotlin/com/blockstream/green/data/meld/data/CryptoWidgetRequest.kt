package com.blockstream.green.data.meld.data

import kotlinx.serialization.Serializable

@Serializable
data class CryptoWidgetRequest(
    val sessionType: String = "BUY",
    val externalCustomerId: String? = null,
    val sessionData: SessionData
)