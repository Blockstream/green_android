package com.blockstream.common.gdk


import kotlinx.serialization.KSerializer
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

abstract class GreenJson<T> {
    open fun encodeDefaultsValues() = true

    open fun explicitNulls() = true

    open fun keepJsonElement() = false

    @Transient
    var jsonElement: JsonElement? = null

    abstract fun kSerializer(): KSerializer<T>

    protected val json by lazy {
        Json {
            encodeDefaults = encodeDefaultsValues()
            explicitNulls = explicitNulls()
            ignoreUnknownKeys = true
        }
    }

    final override fun toString(): String {
        @Suppress("UNCHECKED_CAST")
        return json.encodeToString(kSerializer(), this as T)
    }

    fun toJson() = toString()

    fun toJsonElement(): JsonElement {
        @Suppress("UNCHECKED_CAST")
        return json.encodeToJsonElement(kSerializer(), this as T)
    }

    companion object{
        val json = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }
    }
}