package com.blockstream.gdk.data

import com.blockstream.gdk.GAJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Addressee constructor(
    @SerialName("address") val address: String,
    @SerialName("asset_id") val assetId: String? = null,
    @SerialName("satoshi") val satoshi: Long? = null,
    @SerialName("bip21-params") val bip21Params: Bip21Params? = null,
) : GAJson<Addressee>() {
    override fun kSerializer() = serializer()
}