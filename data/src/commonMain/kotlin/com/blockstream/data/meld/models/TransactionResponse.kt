package com.blockstream.data.meld.models

import kotlinx.serialization.Serializable

@Serializable
data class MeldTransactionResponse(
    val transactions: List<com.blockstream.data.meld.models.MeldTransaction>,
    val count: Long,
    val remaining: Long,
    val totalCount: Long
)