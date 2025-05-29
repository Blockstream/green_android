package com.blockstream.common.looks

import kotlinx.serialization.Serializable

@Serializable
data class AmountAssetLook constructor(
    val amount: String,
    val assetId: String,
    val ticker: String,
    val fiat: String? = null
) {
    val isOutgoing by lazy { amount.startsWith("-") }
}