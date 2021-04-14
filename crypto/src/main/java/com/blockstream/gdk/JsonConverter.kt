package com.blockstream.gdk

import com.blockstream.libgreenaddress.GDK
import mu.KLogging

class JsonConverter : GDK.JSONConverter {
    override fun toJSONObject(jsonString: String?): Any? {
        logger.info { "-> $jsonString" }

        if (jsonString != null && jsonString != "null") {
            // Fix bad json structure decisions eg. empty objects
            val fixedJson = jsonString.replace("{}", "null")
            return GreenWallet.JsonDeserializer.parseToJsonElement(fixedJson)
        }
        return null
    }

    override fun toJSONString(gaJson: Any?): String = gaJson.toString().also {
        logger.info { "<- $it" }
    }

    companion object : KLogging()
}