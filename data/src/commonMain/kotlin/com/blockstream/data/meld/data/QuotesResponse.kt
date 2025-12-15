package com.blockstream.data.meld.data

import kotlinx.serialization.Serializable

@Serializable
data class QuotesResponse(
    val quotes: List<com.blockstream.data.meld.data.QuoteResponse>? = null,
    val message: String? = null,
    val error: String? = null
)