package com.blockstream.gdk.params

import com.blockstream.gdk.GAJson
import com.blockstream.gdk.data.Asset
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Convert(
    @SerialName("satoshi") val satoshi: Long,
    @SerialName("asset_info") val asset: Asset? = null,
) : GAJson<Convert>() {

    override val encodeDefaultsValues: Boolean
        get() = false

    override fun kSerializer(): KSerializer<Convert> {
        return Convert.serializer()
    }
}
