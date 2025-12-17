package com.blockstream.data.swap

import com.blockstream.data.json.SimpleJson
import kotlinx.serialization.Serializable

@Serializable
data class SwapDetails constructor(
    val swapId: String?,
    val address: String,
    val submarineInvoiceTo: String? = null,
    val fromAmount: Long,
    val toAmount: Long? = null,
    val fromAssetId: String,
    val toAssetId: String? = null,
    val providerFee: Long = 0,
    val claimNetworkFee: Long = 0,
) : SimpleJson<SwapDetails>() {
    override fun kSerializer() = serializer()
}