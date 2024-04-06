package com.blockstream.common.gdk.params

import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddressParams constructor(
    @SerialName("address") val address: String,
    @SerialName("satoshi") var satoshi: Long,
    @SerialName("is_greedy") var isGreedy: Boolean = false,
    @SerialName("asset_id") var assetId: String? = null,
) : GreenJson<AddressParams>() {
    override fun encodeDefaultsValues() = false

    override fun kSerializer() = serializer()
}