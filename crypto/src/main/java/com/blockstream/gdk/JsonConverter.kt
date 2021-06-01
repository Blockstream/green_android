package com.blockstream.gdk

import com.blockstream.libgreenaddress.GDK
import mu.KLogging

class JsonConverter(val log: Boolean) : GDK.JSONConverter {
    override fun toJSONObject(jsonString: String?): Any? {
        if(log){
            logger.info { "-> $jsonString" }
        }

        if (jsonString != null && jsonString != "null") {
            return GreenWallet.JsonDeserializer.parseToJsonElement(jsonString)
        }
        return null
    }

    override fun toJSONString(gaJson: Any?): String = gaJson.toString().also {
        if(log) {
            logger.info { "<- $it" }
        }
    }

    companion object : KLogging()
}