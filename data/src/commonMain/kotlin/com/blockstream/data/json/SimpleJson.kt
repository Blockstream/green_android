package com.blockstream.data.json

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

abstract class SimpleJson<T> {
    abstract fun kSerializer(): KSerializer<T>

    final override fun toString(): String {
        @Suppress("UNCHECKED_CAST")
        return toJson()
    }

    fun toJson(): String {
        return DefaultJson.encodeToString(kSerializer(), this as T)
    }

    fun toJsonElement(): JsonElement {
        @Suppress("UNCHECKED_CAST")
        return DefaultJson.encodeToJsonElement(kSerializer(), this as T)
    }

    companion object {
        inline fun <reified T> fromString(jsonString: String?): T? = try {
            DefaultJson.decodeFromString(jsonString ?: "")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        inline fun <reified T> fromJsonElement(jsonElement: JsonElement): T? = try {
            DefaultJson.decodeFromJsonElement(jsonElement)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}