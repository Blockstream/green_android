package com.blockstream.green.data.meld.data

import kotlinx.serialization.Serializable

@Serializable
data class QuotesResponse(
    val quotes: List<QuoteResponse>? = null,
    val message: String? = null,
    val error: String? = null
)