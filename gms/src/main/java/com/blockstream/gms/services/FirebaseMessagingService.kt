package com.blockstream.gms.services

import com.blockstream.common.fcm.FcmCommon
import com.blockstream.common.lightning.BreezNotification
import com.blockstream.common.utils.Loggable
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class FirebaseMessagingService : FirebaseMessagingService(), KoinComponent {

    val fcm: FcmCommon by inject()

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data

        // Check if message contains a data payload.
        if (data.isNotEmpty()) {
            logger.d { "Message data payload: ${remoteMessage.data}" }

            val notificationType = data["notification_type"]

            // Breez Notification
            if (notificationType == "payment_received" || notificationType == "tx_confirmed" || notificationType == "address_txs_confirmed") {
                val xpubHashId = data["app_data"]
                val breezNotification = BreezNotification.fromString(data["notification_payload"])

                if (breezNotification != null && !xpubHashId.isNullOrBlank()) {
                    fcm.handleLightningPushNotification(xpubHashId, breezNotification)
                } else {
                    logger.d { "No notification_payload or app_data" }
                }
            }
        }
    }

    override fun onNewToken(token: String) {
        logger.d { "Refreshed token: $token" }
        fcm.setToken(token)
    }

    companion object: Loggable()
}
