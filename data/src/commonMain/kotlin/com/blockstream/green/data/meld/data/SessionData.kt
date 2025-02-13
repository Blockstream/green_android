package com.blockstream.green.data.meld.data

import kotlinx.serialization.Serializable

@Serializable
data class SessionData(
    val countryCode: String = "US",
    val sourceAmount: String = "200",
    val sourceCurrencyCode: String = "USD",
    val destinationCurrencyCode: String = "BTC",
    val walletAddress: String,
    val serviceProvider: String,
    val redirectUrl: String = "https://blockstream.com/ramps/redirect",
    val externalCustomerId: String? = null
)