package com.blockstream.data.notifications.models

import com.blockstream.data.json.SimpleJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BoltzNotificationSimple(
    val id: String,
    val status: String,
) : SimpleJson<BoltzNotificationSimple>() {
    override fun kSerializer() = serializer()

    companion object Companion {
        fun create(json: Json, data: Map<String, String>): BoltzNotificationSimple {
            // Hack: fix json by replacing single quotes with double quotes
            return json.decodeFromString<BoltzNotificationSimple>(data["data"]?.replace("'", "\"") ?: "")
        }
    }
}
