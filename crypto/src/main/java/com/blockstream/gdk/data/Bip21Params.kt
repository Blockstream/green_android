package com.blockstream.gdk.data

import com.blockstream.gdk.GAJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Bip21Params constructor(
    @SerialName("amount") val amount: String? = null,
    @SerialName("assetid") val assetId: String? = null,
) : GAJson<Bip21Params>() {
    override fun kSerializer() = serializer()

    val hasAssetId : Boolean
        get() = !assetId.isNullOrBlank()

    val hasAmount : Boolean
        get() = !amount.isNullOrBlank()
}