package com.blockstream.green.data.notifications.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class NotificationData(
    val accountId: String,
    val eventId: String,
    val profileId: String,
    val payload: Payload,
    val eventType: String,
    val timestamp: String,
    val version: String,
    val type: NotificationType? = null,
    val title: String? = null,
    val body: String? = null
) {
    companion object {
        fun create(data: Map<String, String>): NotificationData {
            val json = Json { ignoreUnknownKeys = true }

            return NotificationData(
                accountId = data["accountId"] ?: "",
                eventId = data["eventId"] ?: "",
                profileId = data["profileId"] ?: "",
                payload = json.decodeFromString(data["payload"] ?: "{}"),
                eventType = data["eventType"] ?: "",
                timestamp = data["timestamp"] ?: "",
                version = data["version"] ?: "",
                type = NotificationType.valueOfOrUnknown(data["type"]),
                title = data["title"],
                body = data["body"]
            )
        }
    }
}

@Serializable
data class Payload(
    val requestId: String? = null,
    val accountId: String? = null,
    val paymentTransactionId: String? = null,
    val customerId: String? = null,
    val externalCustomerId: String? = null,
    val paymentTransactionStatus: String? = null,
    val transactionType: String? = null,
    val sessionId: String? = null,
)