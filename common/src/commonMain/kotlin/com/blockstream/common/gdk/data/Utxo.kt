package com.blockstream.common.gdk.data

import com.blockstream.common.BTC_POLICY_ASSET
import com.blockstream.common.gdk.GdkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Utxo(
    @SerialName("asset_id") val assetId: String = BTC_POLICY_ASSET, // by default asset_id only available in Liquid
    @SerialName("address_type") val addressType: String,
    @SerialName("block_height") val blockHeight: Long? = null,
    @SerialName("expiry_height") val expiryHeight: Long? = null,
    @SerialName("satoshi") val satoshi: Long,
    @SerialName("txhash") val txHash: String,
    @SerialName("pt_idx") val index: Long,
) : GdkJson<Utxo>() {
    override fun kSerializer() = serializer()
}