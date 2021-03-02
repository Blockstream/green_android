package com.blockstream.gdk.params

import com.blockstream.gdk.GAJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class Limits(
    @SerialName("is_fiat") val isFiat: Boolean,

    @SerialName("bits") val bits: String?,
    @SerialName("btc") val btc: String?,
    @SerialName("fiat") val fiat: String?,
    @SerialName("mbtc") val mbtc: String?,
    @SerialName("sats") val sats: String?,
    @SerialName("ubtc") val ubtc: String?,
): GAJson<Limits>() {

    override fun kSerializer(): KSerializer<Limits> {
        return Limits.serializer()
    }
}
