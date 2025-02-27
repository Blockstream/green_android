package com.blockstream.common.gdk

import com.blockstream.common.utils.Loggable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

class JsonConverter(
    val printGdkMessages: Boolean,
    val maskSensitiveFields: Boolean,
    val appendGdkLogs: (json: String) -> Unit
) {
    private val maskFields = listOf("pin", "mnemonic", "password", "recovery_mnemonic", "seed")

    private fun shouldPrint(jsonString: String?): Boolean{
        if(jsonString == null || jsonString.length > 50_000){
            return false
        }

        if (printGdkMessages) {
            if (
                SkipLogAmountConversions
                && (jsonString.contains("\"is_current\":true,\"mbtc\":\"")
                        || jsonString.startsWith("{\"satoshi\":")
                        || jsonString.startsWith("{\"bits\":"))
            ) {
                return false
            }
        }

        return printGdkMessages
    }

    fun toJSONObject(jsonString: String?): Any? {
        mask(jsonString)?.also { masked ->
            if (shouldPrint(masked)) {
                "▲ $masked".also {
                    logger.i { it }
                }
            }

            appendGdkLogs(masked)
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
            mask(it)?.also { masked ->
                if (shouldPrint(masked)) {
                    "▼ $masked".also {
                        logger.i { it }
                    }
                }
            }
        }
    }

    // Extra protection from logging sensitive information
    fun mask(jsonString: String?): String? {
        var processed = jsonString
        if(maskSensitiveFields) {
            for (mask in maskFields) {
                processed = processed?.replace(Regex("(?<=$mask\":\")(.*?)(?=\")"), "**Redacted**")
            }
        }
        return processed
    }

    companion object : Loggable() {
        const val SkipLogAmountConversions = false

        /**
         * Serialization / Deserialization JSON Options
         */
        val JsonDeserializer = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
}
