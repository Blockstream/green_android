package com.blockstream.common.gdk

import co.touchlab.kermit.Logger
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

class JsonConverter constructor(val log: Boolean, val maskSensitiveFields: Boolean) {
    private val maskFields = listOf("pin", "mnemonic", "password", "recovery_mnemonic", "seed")

    private fun shouldLog(jsonString: String?): Boolean{
        if(jsonString == null || jsonString.length > 50_000){
            return false
        }

        if(log) {
            if (SkipLogAmountConversions && (jsonString.startsWith("{\"satoshi\":") || jsonString.startsWith(
                    "{\"bits\":"
                ))
            ) {
                return false
            }
        }

        return log
    }

    fun toJSONObject(jsonString: String?): Any? {
        if (shouldLog(jsonString)) {
            "▲ ${mask(jsonString)}".let{
                Logger.i { it }
            }
        }

        if (jsonString != null && jsonString != "null") {
            return JsonDeserializer.parseToJsonElement(jsonString)
        }
        return null
    }

    fun toJSONString(any: Any?): String {
        return if(any is JsonElement){
            JsonDeserializer.encodeToString(any)
        }else{
            any.toString()
        }.also {
            if (shouldLog(it)) {
                "▼ ${mask(it)}".let {
                    Logger.i { it }
                }
            }
        }
    }

    // Extra protection from logging sensitive information
    fun mask(jsonString: String?): String? {
        var processed = jsonString
        if(maskSensitiveFields) {
            for (mask in maskFields) {
                processed = processed?.replace(Regex("(?<=$mask\":\")(.*?)(?=\")"), "**masked**")
            }
        }
        return processed
    }

    companion object{
        const val SkipLogAmountConversions = true

        /**
         * Serialization / Deserialization JSON Options
         */
        val JsonDeserializer = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
}
