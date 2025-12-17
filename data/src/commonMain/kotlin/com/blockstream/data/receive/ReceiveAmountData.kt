package com.blockstream.data.receive

data class ReceiveAmountData(
    val isValid: Boolean = false,
    val exchange: String = "",
    val liquidityFee: String? = null,
    val hint: String? = null,
    val error: String? = null
)