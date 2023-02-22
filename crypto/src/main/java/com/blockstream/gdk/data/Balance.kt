package com.blockstream.gdk.data

import android.os.Parcelable
import com.blockstream.gdk.BITS_UNIT
import com.blockstream.gdk.BTC_UNIT
import com.blockstream.gdk.MBTC_UNIT
import com.blockstream.gdk.SATOSHI_UNIT
import com.blockstream.gdk.UBTC_UNIT
import com.blockstream.gdk.params.Convert
import kotlinx.parcelize.Parcelize
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
) : Parcelable {
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

        /**
        Transform the gdk json to a more appropriate format
         */
        fun fromJsonElement(json: Json, element: JsonElement, conversionFrom: Convert): Balance {

            val balance: Balance = json.decodeFromJsonElement(element)
            conversionFrom.asset?.let {
                balance.assetInfo = it
                balance.assetValue = element.jsonObject[conversionFrom.asset.assetId]?.jsonPrimitive?.content
            }

            return balance
        }
    }
}