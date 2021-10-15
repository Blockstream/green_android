package com.blockstream.gdk.params

import com.blockstream.gdk.GAJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddressParams constructor(
    @SerialName("address") val address: String,
    @SerialName("asset_id") var assetId: String? = null,
    @SerialName("satoshi") var satoshi: Long? = null,
) : GAJson<AddressParams>() {
    override val encodeDefaultsValues = false

    override fun kSerializer() = serializer()
}