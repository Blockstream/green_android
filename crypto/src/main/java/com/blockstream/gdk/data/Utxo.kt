package com.blockstream.gdk.data

import com.blockstream.gdk.BTC_POLICY_ASSET
import com.blockstream.gdk.GAJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Utxo(
    @SerialName("asset_id") val assetId: String = BTC_POLICY_ASSET, // by default asset_id only available in Liquid
    @SerialName("address_type") val addressType: String,
    @SerialName("block_height") val blockHeight: Long,
    @SerialName("expiry_height") val expiryHeight: Long? = null,
    @SerialName("satoshi") val satoshi: Long,
    @SerialName("txhash") val txHash: String,
    @SerialName("pt_idx") val index: Long,
) : GAJson<Utxo>() {
    override fun kSerializer(): KSerializer<Utxo> = serializer()
}