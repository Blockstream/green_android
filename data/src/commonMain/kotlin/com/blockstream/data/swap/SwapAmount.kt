package com.blockstream.data.swap

data class SwapAmount constructor(
    val quote: Quote? = null,
    val amountFrom: String = "",
    val amountFromExchange: String? = null,
    val amountTo: String = "",
    val amountToExchange: String? = null,
    val error: String? = null
)
