package com.blockstream.gdk.params

import com.blockstream.gdk.GAJson
import com.blockstream.gdk.data.Asset
import kotlinx.serialization.KSerializer
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
) : GAJson<Convert>() {

    override val encodeDefaultsValues: Boolean
        get() = false

    override fun kSerializer(): KSerializer<Convert> {
        return serializer()
    }

    companion object{
        fun forUnit(unit: String = "btc", amount: String): Convert {

            return when (unit.lowercase()) {
                "btc" -> Convert(btc = amount)
                "mbtc" -> Convert(mbtc = amount)
                "ubtc", "bits", "\u00B5btc" -> Convert(bits = amount)
                "sats" -> Convert(sats = amount)
                else -> Convert(fiat = amount)
            }
        }

        fun forAsset(asset: Asset, amount: String): Convert {
            return Convert(asset = asset, assetAmount = amount)
        }
    }
}
