package com.blockstream.gdk.data

import com.blockstream.gdk.params.Convert
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class Balance(
    @SerialName("bits") val bits: String,
    @SerialName("btc") val btc: String,
    @SerialName("fiat") val fiat: String?,
    @SerialName("fiat_currency") val fiatCurrency: String,
    @SerialName("fiat_rate") val fiatRate: String?,
    @SerialName("is_fiat") val isFiat: Boolean = false, // this options is only available in 2fa threshold
    @SerialName("mbtc") val mbtc: String,
    @SerialName("satoshi") val satoshi: Long,
    @SerialName("sats") val sats: String,
    @SerialName("ubtc") val ubtc: String,
){
    var assetValue : String? = null
    var assetInfo : Asset? = null

    fun getFiatValue() = "%s %s".format(fiat ?: "n/a", fiatCurrency)

    fun getValue(unit: String): String {
        return when(unit.lowercase()){
            "\u00B5btc", "ubtc" -> ubtc
            "mbtc" -> mbtc
            "bits" -> bits
            "sats" -> sats
            "btc" -> btc
            else -> fiat ?: "n/a"
        }
    }

    companion object {
        /**
        Transform the gdk json to a more appropriate format
         */
        fun fromJsonElement(json: Json, element: JsonElement, conversionFrom: Convert): Balance {

            val balance : Balance = json.decodeFromJsonElement(element)
            conversionFrom.asset?.let {
                balance.assetInfo = it
                balance.assetValue = element.jsonObject[conversionFrom.asset.assetId]?.jsonPrimitive?.content
            }

            return balance
        }
    }
}