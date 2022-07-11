package com.blockstream.gdk

import com.blockstream.libgreenaddress.GDK
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import mu.KLogging

interface Logger{
    fun log(message: String)
}

class JsonConverter constructor(val log: Boolean, val maskSensitiveFields: Boolean, private val extraLogger: Logger? = null) : GDK.JSONConverter {
    private val maskFields = listOf("pin", "mnemonic", "password", "recovery_mnemonic", "seed")

    private val jsonSerializer by lazy { Json {  } }

    override fun toJSONObject(jsonString: String?): Any? {
        if (log) {
            "▲ ${mask(jsonString)}".let{
                logger.info { it }
                extraLogger?.log(it)
            }
        }

        if (jsonString != null && jsonString != "null") {
            return GreenWallet.JsonDeserializer.parseToJsonElement(jsonString)
        }
        return null
    }

    override fun toJSONString(any: Any?): String {
        return if(any is JsonElement){
            jsonSerializer.encodeToString(any)
        }else{
            any.toString()
        }.also {
            if (log) {
                "▼ ${mask(it)}".let {
                    logger.info { it }
                    extraLogger?.log(it)
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

    companion object : KLogging()
}
