package com.blockstream.common.gdk.params

import com.blockstream.common.BITS_UNIT
import com.blockstream.common.BTC_UNIT
import com.blockstream.common.MBTC_UNIT
import com.blockstream.common.SATOSHI_UNIT
import com.blockstream.common.UBTC_UNIT
import com.blockstream.common.gdk.GdkJson
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
) : GdkJson<Limits>() {
    override val encodeDefaultsValues = false

    override fun kSerializer() = serializer()

    companion object {
        fun fromUnit(unit: String, value: String) =
            when (unit) {
                BTC_UNIT -> Limits(btc = value, isFiat = false)
                MBTC_UNIT -> Limits(mbtc = value, isFiat = false)
                UBTC_UNIT -> Limits(ubtc = value, isFiat = false)
                BITS_UNIT -> Limits(bits = value, isFiat = false)
                SATOSHI_UNIT -> Limits(sats = value, isFiat = false)
                else -> Limits(fiat = value, isFiat = true)
            }

    }
}
