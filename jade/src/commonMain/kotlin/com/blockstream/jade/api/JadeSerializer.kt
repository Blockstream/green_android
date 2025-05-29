package com.blockstream.jade.api

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

@OptIn(ExperimentalSerializationApi::class)
abstract class JadeSerializer<T> {
    open fun encodeDefaultsValues() = true

    open fun explicitNulls() = true

    abstract fun kSerializer(): KSerializer<T>

    private val json
        get() = Json {
            encodeDefaults = encodeDefaultsValues()
            explicitNulls = explicitNulls()
            ignoreUnknownKeys = true
        }

    @Suppress("UNCHECKED_CAST")
    fun toJson(): String = json.encodeToString(kSerializer(), this as T)

    @Suppress("UNCHECKED_CAST")
    fun toJsonElement(): JsonElement = json.encodeToJsonElement(kSerializer(), this as T)

    @Suppress("UNCHECKED_CAST")
    fun toCbor() = Cbor {
        encodeDefaults = encodeDefaultsValues()
        ignoreUnknownKeys = true
        useDefiniteLengthEncoding = true
        alwaysUseByteString = true
    }.encodeToByteArray(kSerializer(), this as T)

    @ExperimentalStdlibApi
    fun toCborHex() = toCbor().toHexString()

    companion object {
        val JadeJsonDeserializer by lazy { Json { ignoreUnknownKeys = true } }
        val JadeCborDeserializer by lazy {
            Cbor {
                ignoreUnknownKeys = true
                alwaysUseByteString = true
            }
        }

        inline fun <reified T : JadeSerializer<*>> decode(jsonString: String): T =
            JadeJsonDeserializer.decodeFromString(jsonString)

        inline fun <reified T : JadeSerializer<*>> decode(cbor: ByteArray): T =
            JadeCborDeserializer.decodeFromByteArray(cbor)

        inline fun <reified T : JadeSerializer<*>> decodeOrNull(cbor: ByteArray): T? = try {
            JadeCborDeserializer.decodeFromByteArray(cbor)
        } catch (e: Exception) {
            null
        }

        fun <T> decodeOrNull(serializer: DeserializationStrategy<T>, cbor: ByteArray): T? = try {
            JadeCborDeserializer.decodeFromByteArray(serializer, cbor)
        } catch (e: Exception) {
            null
        }

        private var _id = 1000
        fun jadeId() = (_id++).toString()
    }
}





