package com.blockstream.gdk.data

import com.blockstream.gdk.GAJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mu.KLogging

@Serializable
data class Settings(
    @SerialName("altimeout") val altimeout: Int,
    @SerialName("csvtime") val csvTime: Int = 0,
    @SerialName("nlocktime") val nlocktime: Int = 0,
    @SerialName("notifications") val notifications: SettingsNotification? = null,
    @SerialName("pricing") val pricing: Pricing,
    @SerialName("required_num_blocks") val requiredNumBlocks: Int,
    @SerialName("unit") val unit: String,
    @SerialName("pgp") val pgp: String? = null,

): GAJson<Settings>() {
    override val encodeDefaultsValues: Boolean = false

    override fun kSerializer() = serializer()

    companion object: KLogging(){
        fun normalizeFromProminent(networkSettings: Settings, prominentSettings: Settings, pgpFromProminent: Boolean = false): Settings{
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

@Serializable
data class SettingsNotification(
    @SerialName("email_incoming") val emailIncoming: Boolean,
    @SerialName("email_outgoing") val emailOutgoing: Boolean,
): GAJson<SettingsNotification>() {

    override fun kSerializer(): KSerializer<SettingsNotification> {
        return serializer()
    }
}

@Serializable
data class Pricing(
    @SerialName("currency") val currency: String,
    @SerialName("exchange") val exchange: String,
): GAJson<Pricing>() {

    override fun kSerializer(): KSerializer<Pricing> {
        return serializer()
    }

    fun toIdentifiable() = String.format("%s %s", currency, exchange)

    companion object {
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
