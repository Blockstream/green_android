package com.blockstream.common.gdk


import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Transient
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.kotlincrypto.hash.sha2.SHA256

@OptIn(ExperimentalSerializationApi::class)
abstract class GreenJson<T> {
    open fun encodeDefaultsValues() = true

    open fun explicitNulls() = true

    open fun keepJsonElement() = false

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

    fun sha256(): String = SHA256().digest(toJson().encodeToByteArray()).toHexString()

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

    open fun processJsonElement() { }

    companion object {
        val json = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }
    }
}