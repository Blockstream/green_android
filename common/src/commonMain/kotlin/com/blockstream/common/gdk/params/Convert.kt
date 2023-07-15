package com.blockstream.common.gdk.params

import com.blockstream.common.gdk.GreenJson
import com.blockstream.common.gdk.data.Asset
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Convert constructor(
    @SerialName("satoshi") val satoshi: Long? = null,
    @SerialName("asset_info") val asset: Asset? = null,

    @SerialName("assetAmount") val assetAmount: String? = null, // this field is only in Android app

    @SerialName("btc") val btc: String? = null,
    @SerialName("mbtc") val mbtc: String? = null,
    @SerialName("bits") val bits: String? = null,
    @SerialName("sats") val sats: String? = null,

    @SerialName("fiat") val fiat: String? = null,

    // Fallback to avoid blocking convert_amount call
    @SerialName("fiat_currency") val fiatCurrency: String? = "USD",
    @SerialName("fiat_rate") val fiatRate: String? = "0",
) : GreenJson<Convert>() {

    override fun encodeDefaultsValues() = false

    override fun kSerializer() = serializer()

    companion object{
        fun forUnit(unit: String = com.blockstream.common.BTC_UNIT, amount: String): Convert {

            return when (unit) {
                com.blockstream.common.BTC_UNIT -> Convert(btc = amount)
                com.blockstream.common.MBTC_UNIT -> Convert(mbtc = amount)
                com.blockstream.common.BITS_UNIT, com.blockstream.common.UBTC_UNIT -> Convert(bits = amount)
                com.blockstream.common.SATOSHI_UNIT -> Convert(sats = amount)
                else -> Convert(fiat = amount)
            }
        }

        fun forAsset(asset: Asset, amount: String): Convert {
            return Convert(asset = asset, assetAmount = amount)
        }
    }
}
