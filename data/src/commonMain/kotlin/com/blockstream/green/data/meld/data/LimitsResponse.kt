package com.blockstream.green.data.meld.data

import kotlinx.serialization.Serializable

@Serializable
data class LimitsResponse(
    val currencyCode: String,
    val defaultAmount: Double? = null,
    val minAmount: Double = 0.0,
    val maxAmount: Double = 0.0
)