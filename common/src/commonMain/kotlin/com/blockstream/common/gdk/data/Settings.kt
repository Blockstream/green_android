package com.blockstream.common.gdk.data

import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.BITS_UNIT
import com.blockstream.common.BTC_UNIT
import com.blockstream.common.BitcoinUnits
import com.blockstream.common.MBTC_UNIT
import com.blockstream.common.SATOSHI_UNIT
import com.blockstream.common.TestnetUnits
import com.blockstream.common.UBTC_UNIT
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Parcelize
@Serializable
data class Settings(
    @SerialName("altimeout") val altimeout: Int = 0, // minutes
    @SerialName("csvtime") val csvTime: Int = 0,
    @SerialName("nlocktime") val nlocktime: Int = 0,
    @SerialName("notifications") val notifications: SettingsNotification? = null,
    @SerialName("pricing") val pricing: Pricing,
    @SerialName("required_num_blocks") val requiredNumBlocks: Int = 12,
    @SerialName("unit") val unit: String,
    @SerialName("pgp") val pgp: String? = null
): GreenJson<Settings>(), Parcelable {
    override fun encodeDefaultsValues() = false

    override fun kSerializer() = serializer()

    fun forWalletExtras(): Settings{
        return Settings(
            altimeout = altimeout,
            pricing = pricing,
            unit = unit
        )
    }

    fun networkUnit(session: GdkSession): String = unit.let {
        if (session.isTestnet) {
            when (it) {
                BTC_UNIT -> "TEST"
                MBTC_UNIT -> "mTEST"
                UBTC_UNIT -> "\u00B5TEST"
                BITS_UNIT -> "bTEST"
                SATOSHI_UNIT -> "sTEST"
                else -> unit
            }
        } else {
            it
        }
    }.let {
        if (session.defaultNetworkOrNull?.isLiquid == true) {
            "L-$it"
        } else {
            it
        }
    }


    companion object {
        fun fromNetworkUnit(unit: String, session: GdkSession): String = if (session.isTestnet) {
            BitcoinUnits.getOrNull(TestnetUnits.indexOf(unit.replace("L-", ""))) ?: BTC_UNIT
        } else {
            unit
        }

        fun normalizeFromProminent(networkSettings: Settings, prominentSettings: Settings, pgpFromProminent: Boolean = false): Settings {
            return Settings(
                // Prominent Settings
                altimeout = prominentSettings.altimeout,
                pricing = prominentSettings.pricing,
                unit = prominentSettings.unit,
                pgp = prominentSettings.pgp.takeIf { pgpFromProminent } ?: networkSettings.pgp.takeIf { !pgpFromProminent },

                // Network Settings
                csvTime = networkSettings.csvTime,
                nlocktime = networkSettings.nlocktime,
                notifications = networkSettings.notifications,
                requiredNumBlocks = networkSettings.requiredNumBlocks
            )
        }
    }
}

@Parcelize
@Serializable
data class SettingsNotification(
    @SerialName("email_incoming") val emailIncoming: Boolean,
    @SerialName("email_outgoing") val emailOutgoing: Boolean,
): GreenJson<SettingsNotification>(), Parcelable {

    override fun kSerializer() = serializer()
}

@Parcelize
@Serializable
data class Pricing(
    @SerialName("currency") val currency: String,
    @SerialName("exchange") val exchange: String,
): GreenJson<Pricing>(), Parcelable {

    override fun kSerializer() = serializer()

    fun toIdentifiable() = "$currency $exchange"

    companion object {
        fun fromString(jsonString: String): List<Pricing> {
            return fromJsonElement(json.parseToJsonElement(jsonString))
        }

        /**
        Transform the gdk json to a more appropriate format
         */
        fun fromJsonElement(element: JsonElement): List<Pricing> {
            val list = mutableListOf<Pricing>()

            val exchanges = element.jsonObject["per_exchange"]?.jsonObject
            exchanges?.keys?.let{
                for (exchange in it){
                    exchanges.jsonObject[exchange]?.jsonArray?.let{
                        for(currency in it){
                            list += Pricing(currency =  currency.jsonPrimitive.content, exchange = exchange )
                        }
                    }
                }
            }

            list.sortBy { it.currency }

            return list.toList()
        }
    }
}

fun String.asPricing(): Pricing? {
    try{
        val split = this.split(" ")
        return Pricing(currency = split[0], exchange = split[1])
    }catch (e :Exception){
        e.printStackTrace()
    }

    return null
}
