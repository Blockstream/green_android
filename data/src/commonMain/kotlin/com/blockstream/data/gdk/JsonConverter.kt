package com.blockstream.data.gdk

import com.blockstream.utils.Loggable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class JsonConverter(
    val printGdkMessages: Boolean,
    val maskSensitiveFields: Boolean,
    val appendGdkLogs: (json: String) -> Unit
) {
    private val maskFields =
        listOf(
            "pin",
            "mnemonic",
            "password",
            "recovery_mnemonic",
            "seed",
            "bip39_passphrase",
            "private_key",
            "device_key",
            "master_blinding_key",
            "pin_data",
            "encrypted_data",
            "core_descriptors",
            "slip132_extended_pubkey",
            "slip132_extended_pubkeys",
            "xpub",
            "xpubs",
            "amountblinder",
            "amountblinders",
            "assetblinder",
            "assetblinders",
            "blinding_key",
            "blinding_nonce",
            "nonces",
            "address",
            "addresses",
            "addressee",
            "addressees",
            "script",
            "scripts",
            "scriptpubkey",
            "txhash",
            "txid",
            "gauth"
        )

    private fun shouldPrint(jsonString: String?): Boolean {
        if (jsonString == null || jsonString.length > 50_000) {
            return false
        }

        if (printGdkMessages) {
            if (
                SkipLogAmountConversions
                && (jsonString.contains("\"is_current\":true,\"mbtc\":\"")
                        || jsonString.startsWith("{\"satoshi\":")
                        || jsonString.startsWith("{\"bits\":"))
            ) {
                return false
            }
        }

        return printGdkMessages
    }

    fun toJSONObject(jsonString: String?): Any? {
        mask(jsonString)?.also { masked ->
            if (shouldPrint(masked)) {
                "▲ $masked".also {
                    logger.i { it }
                }
            }

            appendGdkLogs(masked)
        }

        if (jsonString != null && jsonString != "null") {
            return JsonDeserializer.parseToJsonElement(jsonString)
        }

        return null
    }

    fun toJSONString(any: Any?): String {
        return if (any is JsonElement) {
            JsonDeserializer.encodeToString(any)
        } else {
            any.toString()
        }.also {
            mask(it)?.also { masked ->
                if (shouldPrint(masked)) {
                    "▼ $masked".also {
                        logger.i { it }
                    }
                }
            }
        }
    }

    // Extra protection from logging sensitive information
    fun mask(jsonString: String?): String? {
        if (!maskSensitiveFields || jsonString == null) {
            return jsonString
        }

        // Redacting over the parsed tree masks a value whole, whatever its type and however it is
        // escaped or spaced. Payloads that are not valid JSON cannot be redacted precisely, so they
        // are dropped as a whole rather than risking a partially masked secret.
        return try {
            JsonDeserializer.encodeToString(JsonDeserializer.parseToJsonElement(jsonString).redact())
        } catch (_: Exception) {
            maskText(jsonString)
        }
    }

    private fun JsonElement.redact(): JsonElement = when (this) {
        is JsonObject -> JsonObject(mapValues { (key, value) -> if (isSensitive(key)) Redacted else value.redact() })
        is JsonArray -> JsonArray(map { it.redact() })
        else -> this
    }

    private fun isSensitive(key: String) = maskFields.any { key.endsWith(it) }

    private fun maskText(jsonString: String): String =
        if (maskFields.any { Regex("\"\\w*$it\"\\s*:").containsMatchIn(jsonString) }) RedactedValue else jsonString

    companion object : Loggable(bucket = null) {
        const val SkipLogAmountConversions = true

        private const val RedactedValue = "**Redacted**"
        private val Redacted = JsonPrimitive(RedactedValue)

        /**
         * Serialization / Deserialization JSON Options
         */
        val JsonDeserializer = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
}
