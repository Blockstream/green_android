package com.blockstream.gdk


import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

abstract class GAJson<T> {
    open val encodeDefaultsValues = true
    open val keepJsonElement = false

    var jsonElement: JsonElement? = null

    abstract fun kSerializer(): KSerializer<T>

    protected val json by lazy { Json { encodeDefaults = encodeDefaultsValues; ignoreUnknownKeys = true} }

    final override fun toString(): String {
        @Suppress("UNCHECKED_CAST")
        return json.encodeToString(kSerializer(), this as T)
    }

    fun toJson() = toString()

    fun toJsonElement(): JsonElement {
        @Suppress("UNCHECKED_CAST")
        return json.encodeToJsonElement(kSerializer(), this as T)
    }
}