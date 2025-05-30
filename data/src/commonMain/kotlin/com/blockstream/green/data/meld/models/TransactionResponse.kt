package com.blockstream.green.data.meld.models

import kotlinx.serialization.Serializable

@Serializable
data class MeldTransactionResponse(
    val transactions: List<MeldTransaction>,
    val count: Long,
    val remaining: Long,
    val totalCount: Long
)