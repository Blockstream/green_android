package com.blockstream.gdk.data

import com.blockstream.gdk.GAJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.*

@Serializable
data class Settings(
    @SerialName("altimeout") val altimeout: Int,
    @SerialName("csvtime") val csvtime: Int = 0,
    @SerialName("nlocktime") val nlocktime: Int = 0,
    @SerialName("notifications") val notifications: Map<String, Boolean>? = null,
    @SerialName("pricing") val pricing: Pricing,
    @SerialName("required_num_blocks") val requiredNumBlocks: Int,
    @SerialName("unit") val unit: String,
    @SerialName("pgp") val pgp: String? = null,
): GAJson<Settings>() {

    override fun kSerializer(): KSerializer<Settings> {
        return serializer()
    }

    val unitKey : String
        get() {
            return unit.lowercase(Locale.ROOT).replace("\u00B5", "u")
        }

}

@Serializable
data class Pricing(
    @SerialName("currency") val currency: String,
    @SerialName("exchange") val exchange: String,
): GAJson<Pricing>() {

    override fun kSerializer(): KSerializer<Pricing> {
        return Pricing.serializer()
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
