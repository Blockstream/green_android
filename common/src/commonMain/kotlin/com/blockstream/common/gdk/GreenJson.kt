package com.blockstream.common.gdk


import cafe.adriel.voyager.core.lifecycle.JavaSerializable
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Transient
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

@OptIn(ExperimentalSerializationApi::class)
abstract class GreenJson<T>: JavaSerializable {
    open fun encodeDefaultsValues() = true

    open fun explicitNulls() = true

    open fun keepJsonElement() = false

    @kotlin.jvm.Transient
    @Transient
    var jsonElement: JsonElement? = null

    abstract fun kSerializer(): KSerializer<T>

    protected val json
        get() = Json {
            encodeDefaults = encodeDefaultsValues()
            explicitNulls = explicitNulls()
            ignoreUnknownKeys = true
        }

    final override fun toString(): String {
        @Suppress("UNCHECKED_CAST")
        return json.encodeToString(kSerializer(), this as T)
    }

    fun toJson() = toString()

    @Suppress("UNCHECKED_CAST")
    fun toCbor() = Cbor {
        encodeDefaults = encodeDefaultsValues()
        ignoreUnknownKeys = true
    }.encodeToByteArray(kSerializer(), this as T)

    @ExperimentalStdlibApi
    fun toCborHex() = toCbor().toHexString()

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