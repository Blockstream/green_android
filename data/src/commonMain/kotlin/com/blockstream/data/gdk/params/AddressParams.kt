package com.blockstream.data.gdk.params

import com.blockstream.data.gdk.GreenJson
import com.blockstream.data.gdk.data.Address
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddressParams constructor(
    @SerialName("address")
    val address: String,
    @SerialName("satoshi")
    var satoshi: Long,
    @SerialName("is_greedy")
    var isGreedy: Boolean = false,
    @SerialName("asset_id")
    var assetId: String? = null,
    // Those fields are used on Redeposit only
    val receiveAddress: Address? = null,
) : GreenJson<AddressParams>() {
    override fun encodeDefaultsValues() = false

    override fun kSerializer() = serializer()
}