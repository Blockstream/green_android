package com.blockstream.data.meld.data

import kotlinx.serialization.Serializable

@Serializable
data class CryptoQuoteRequest(
    val countryCode: String = "US",
    val sourceAmount: String = "200",
    val sourceCurrencyCode: String = "USD",
    val destinationCurrencyCode: String = "BTC",
    val externalCustomerId: String? = null
)