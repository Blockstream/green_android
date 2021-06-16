package com.blockstream.gdk.params

import com.blockstream.gdk.GAJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class Limits(
    @SerialName("is_fiat") val isFiat: Boolean,

    @SerialName("btc") val btc: String? = null,
    @SerialName("mbtc") val mbtc: String? = null,
    @SerialName("ubtc") val ubtc: String? = null,
    @SerialName("bits") val bits: String? = null,
    @SerialName("sats") val sats: String? = null,
    @SerialName("fiat") val fiat: String? = null,
) : GAJson<Limits>() {
    override val encodeDefaultsValues = false

    override fun kSerializer(): KSerializer<Limits> {
        return serializer()
    }

    companion object {
        fun fromUnit(unit: String, value: String) =
            when (unit) {
                "btc" -> Limits(btc = value, isFiat = false)
                "mbtc" -> Limits(mbtc = value, isFiat = false)
                "ubtc" -> Limits(ubtc = value, isFiat = false)
                "bits" -> Limits(bits = value, isFiat = false)
                "sats" -> Limits(sats = value, isFiat = false)
                else -> Limits(fiat = value, isFiat = true)
            }

    }
}
