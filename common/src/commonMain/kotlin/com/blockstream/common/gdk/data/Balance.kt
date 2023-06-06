package com.blockstream.common.gdk.data

import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.BITS_UNIT
import com.blockstream.common.BTC_UNIT
import com.blockstream.common.MBTC_UNIT
import com.blockstream.common.SATOSHI_UNIT
import com.blockstream.common.UBTC_UNIT
import com.blockstream.common.gdk.GdkJson
import com.blockstream.common.gdk.params.Convert
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

/*
    GDK 0.0.58.post1 changed the limits structure to return only fiat values if is_fiat = true, so
    btc amounts had to have default values if they don't exists
 */
@Parcelize
@Serializable
data class Balance constructor(
    @SerialName("bits") val bits: String = "0.00",
    @SerialName("btc") val btc: String = "0.00000000",
    @SerialName("fiat") val fiat: String? = null,
    @SerialName("fiat_currency") val fiatCurrency: String? = null,
    @SerialName("fiat_rate") val fiatRate: String? = null,
    @SerialName("is_fiat") val isFiat: Boolean = false, // this options is only available in 2fa threshold
    @SerialName("mbtc") val mbtc: String = "0.00000",
    @SerialName("satoshi") val satoshi: Long = 0,
    @SerialName("sats") val sats: String = "0",
    @SerialName("ubtc") val ubtc: String = "0.00",
    @SerialName("is_current") val isCurrent: Boolean? = null,
    var assetValue: String? = null,
    var assetInfo: Asset? = null
): GdkJson<Balance>(), Parcelable {
    override fun kSerializer() = serializer()

    val valueInMainUnit: String get() = assetValue ?: btc

    fun getValue(unit: String): String {
        return when (unit) {
            UBTC_UNIT, "uBTC" -> ubtc
            MBTC_UNIT -> mbtc
            BITS_UNIT -> bits
            SATOSHI_UNIT -> sats
            BTC_UNIT -> btc
            else -> fiat ?: "n/a"
        }
    }

    companion object {
        fun fromAssetWithoutMetadata(convert: Convert): Balance {
            return Balance(
                satoshi = convert.satoshi ?: 0,
                bits = "",
                btc = "",
                fiatCurrency = "",
                mbtc = "",
                sats = "",
                ubtc = ""
            )
        }

        fun fromJsonString(jsonString: String, conversionFrom: Convert): Balance {
            return fromJsonElement(json.parseToJsonElement(jsonString), conversionFrom)
        }

        /**
        Transform the gdk json to a more appropriate format
         */
        fun fromJsonElement(element: JsonElement, conversionFrom: Convert): Balance {

            val balance: Balance = json.decodeFromJsonElement(element)
            conversionFrom.asset?.let {
                balance.assetInfo = it
                balance.assetValue = element.jsonObject[conversionFrom.asset.assetId]?.jsonPrimitive?.content
            }

            return balance
        }
    }
}