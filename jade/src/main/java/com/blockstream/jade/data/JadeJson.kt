package com.blockstream.jade.data

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json


abstract class JadeJson<T> {

    companion object{
        val JadeJsonDeserializer by lazy { Json { ignoreUnknownKeys = true} }

        inline fun <reified T: JadeJson<*>> decode(jsonString: String): T = JadeJsonDeserializer.decodeFromString(jsonString)
    }
}