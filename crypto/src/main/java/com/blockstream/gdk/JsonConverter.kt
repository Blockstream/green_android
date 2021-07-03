package com.blockstream.gdk

import com.blockstream.libgreenaddress.GDK
import mu.KLogging


class JsonConverter(val log: Boolean, val maskSensitiveFields: Boolean) : GDK.JSONConverter {
    val maskFields = listOf("pin", "mnemonic", "password", "recovery_xpub", "recovery_chain_code")

    override fun toJSONObject(jsonString: String?): Any? {
        if (log) {
            logger.info { "-> ${mask(jsonString)}" }
        }

        if (jsonString != null && jsonString != "null") {
            return GreenWallet.JsonDeserializer.parseToJsonElement(jsonString)
        }
        return null
    }

    override fun toJSONString(gaJson: Any?): String = gaJson.toString().also {
        if (log) {
            logger.info { "<- ${mask(it)}" }
        }
    }

    // Extra protection from logging sensitive information
    private fun mask(jsonString: String?): String? {
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
