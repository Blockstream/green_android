package com.blockstream.gdk.data

import com.blockstream.gdk.GAJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Utxo(
    @SerialName("address_type") val addressType: String,
    @SerialName("block_height") val blockHeight: Long,
    @SerialName("expiry_height") val expiryHeight: Long? = null,
    @SerialName("satoshi") val satoshi: Long,
    @SerialName("txhash") val txHash: String,
) : GAJson<Utxo>() {
    override fun kSerializer(): KSerializer<Utxo> = serializer()
}