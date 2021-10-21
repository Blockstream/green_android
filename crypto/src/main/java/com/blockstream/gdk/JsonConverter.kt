package com.blockstream.gdk

import com.blockstream.libgreenaddress.GDK
import mu.KLogging

interface Logger{
    fun log(message: String)
}

class JsonConverter(val log: Boolean, val maskSensitiveFields: Boolean, private val extraLogger: Logger? = null) : GDK.JSONConverter {
    private val maskFields = listOf("pin", "mnemonic", "password", "recovery_mnemonic")

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

    override fun toJSONString(gaJson: Any?): String = gaJson.toString().also {
        if (log) {
            "▼ ${mask(it)}".let {
                logger.info { it }
                extraLogger?.log(it)
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
