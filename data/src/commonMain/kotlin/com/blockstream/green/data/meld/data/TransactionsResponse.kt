package com.blockstream.green.data.meld.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class TransactionsResponse(
    val count: Long,
    val remaining: Long,
    val totalCount: Long,
    val transactions: List<JsonElement>
)