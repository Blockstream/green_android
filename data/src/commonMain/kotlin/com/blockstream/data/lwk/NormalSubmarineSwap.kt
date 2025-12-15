package com.blockstream.data.lwk

import com.blockstream.data.json.SimpleJson
import kotlinx.serialization.Serializable
import lwk.PreparePayResponse

@Serializable
data class NormalSubmarineSwap constructor(
    val swapId: String? = null,
    val address: String,
    val satoshi: Long,
    // For display purposes
    val bolt11Invoice: String,
    val recipientReceivesSatoshi: Long = satoshi,
    val serviceFee: Long = 0
) : SimpleJson<NormalSubmarineSwap>() {
    override fun kSerializer() = serializer()

    companion object {
        fun from(invoice: String, pay: PreparePayResponse): NormalSubmarineSwap {
            return NormalSubmarineSwap(
                swapId = pay.swapId(),
                address = pay.uriAddress().toString(),
                satoshi = pay.uriAmount().toLong(),
                bolt11Invoice = invoice,
                recipientReceivesSatoshi = pay.uriAmount().toLong().let {
                    it - (pay.fee()?.toLong() ?: 0)
                },
                serviceFee = pay.fee()?.toLong() ?: 0
            )
        }
    }
}