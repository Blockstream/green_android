package com.blockstream.common.gdk.data

import com.blockstream.common.gdk.GdkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Bip21Params constructor(
    @SerialName("amount") val amount: String? = null,
    @SerialName("assetid") val assetId: String? = null,
) : GdkJson<Bip21Params>() {
    override fun kSerializer() = serializer()

    val hasAssetId : Boolean
        get() = !assetId.isNullOrBlank()

    val hasAmount : Boolean
        get() = !amount.isNullOrBlank()
}