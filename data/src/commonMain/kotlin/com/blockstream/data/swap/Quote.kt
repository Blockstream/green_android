package com.blockstream.data.swap

import kotlinx.serialization.Serializable

@Serializable
data class Quote(
    val sendAmount: Long,
    val receiveAmount: Long,
    val claimNetworkFee: Long,
    val boltzFee: Long,
    val minimal: Long,
    val maximal: Long
) {

    fun isValid(amount: Long): QuoteValidity = when {
        amount < minimal -> QuoteValidity.MIN
        amount > maximal -> QuoteValidity.MAX
        else -> QuoteValidity.VALID
    }

    companion object {
        fun from(quote: lwk.Quote) = Quote(
            sendAmount = quote.sendAmount.toLong(),
            receiveAmount = quote.receiveAmount.toLong(),
            claimNetworkFee = quote.networkFee.toLong(),
            boltzFee = quote.boltzFee.toLong(),
            minimal = quote.min.toLong(),
            maximal = quote.max.toLong()
        )
    }
}