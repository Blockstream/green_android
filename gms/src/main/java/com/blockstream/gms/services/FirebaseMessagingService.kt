package com.blockstream.gms.services

import com.blockstream.data.extensions.tryCatchNull
import com.blockstream.data.fcm.FcmCommon
import com.blockstream.data.notifications.models.BoltzNotificationSimple
import com.blockstream.data.notifications.models.MeldNotificationData
import com.blockstream.utils.Loggable
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FirebaseMessagingService : FirebaseMessagingService(), KoinComponent {

    val fcm: FcmCommon by inject()
    val json: Json by inject()

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        logger.d { "Received message: ${remoteMessage.data}" }
        val data = remoteMessage.data

        if (data.isEmpty()) return

        when (data["type"]) {
            "BOLTZ_EVENT" -> {
                tryCatchNull {
                    val boltzNotificationData = BoltzNotificationSimple.create(json, data)
                    fcm.handleBoltzPushNotification(boltzNotificationData)
                }
            }

            "GREENLIGHT_EVENT" -> {
                val xpubHashId = data["wallet_hashed_id"]

                if (!xpubHashId.isNullOrBlank()) {
                    fcm.handleGreenlightPushNotification(xpubHashId = xpubHashId)
                } else {
                    logger.d { "Greenlight push missing wallet_hashed_id $data" }
                }
            }

            "MELD_TRANSACTION" -> {
                tryCatchNull {
                    val notification = MeldNotificationData.create(remoteMessage.data)

                    fcm.showBuyTransactionNotification(notification)
                }
            }

            else -> {
                logger.d { "Unhandled notification type ${data["type"]}" }
            }
        }
    }

    override fun onNewToken(token: String) {
        logger.d { "Refreshed token: $token" }
        fcm.setToken(token)
    }

    companion object : Loggable()
}

