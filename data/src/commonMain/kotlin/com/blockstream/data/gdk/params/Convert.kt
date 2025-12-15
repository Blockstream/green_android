package com.blockstream.data.gdk.params

import com.blockstream.data.BITS_UNIT
import com.blockstream.data.BTC_UNIT
import com.blockstream.data.MBTC_UNIT
import com.blockstream.data.SATOSHI_UNIT
import com.blockstream.data.UBTC_UNIT
import com.blockstream.data.gdk.GreenJson
import com.blockstream.data.gdk.data.Asset
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Convert constructor(
    @SerialName("satoshi")
    val satoshi: Long? = null,
    @SerialName("asset_info")
    val asset: Asset? = null,

    @SerialName("btc")
    val btc: String? = null,
    @SerialName("mbtc")
    val mbtc: String? = null,
    @SerialName("bits")
    val bits: String? = null,
    @SerialName("sats")
    val sats: String? = null,

    @SerialName("fiat")
    val fiat: String? = null,

    // Fallback to avoid blocking convert_amount call
    @SerialName("fiat_currency")
    val fiatCurrency: String? = "USD",
    @SerialName("fiat_rate")
    val fiatRate: String? = "0",
) : GreenJson<Convert>() {

    override fun encodeDefaultsValues() = false

    override fun kSerializer() = serializer()

    companion object {
        fun create(
            isPolicyAsset: Boolean,
            asset: Asset? = null,
            asString: String? = null,
            asLong: Long? = null,
            unit: String = BTC_UNIT
        ): Convert {
            return if (isPolicyAsset || asset == null) {
                if (asString != null) {
                    when (unit) {
                        BTC_UNIT -> Convert(btc = asString)
                        MBTC_UNIT -> Convert(mbtc = asString)
                        BITS_UNIT, UBTC_UNIT -> Convert(bits = asString)
                        SATOSHI_UNIT -> Convert(sats = asString)
                        else -> Convert(fiat = asString)
                    }
                } else {
                    Convert(satoshi = asLong)
                }
            } else {
                if (asString != null) {
                    Convert(asset = asset, btc = asString)
                } else {
                    Convert(asset = asset, satoshi = asLong)
                }
            }
        }
    }
}
