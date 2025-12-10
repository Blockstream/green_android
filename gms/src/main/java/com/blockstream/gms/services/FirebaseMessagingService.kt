package com.blockstream.gms.services

import com.blockstream.common.extensions.tryCatchNull
import com.blockstream.common.fcm.FcmCommon
import com.blockstream.common.lightning.BreezNotification
import com.blockstream.green.data.config.AppInfo
import com.blockstream.green.data.notifications.models.BoltzNotificationSimple
import com.blockstream.green.data.notifications.models.MeldNotificationData
import com.blockstream.green.data.notifications.models.MeldNotificationType
import com.blockstream.green.utils.Loggable
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FirebaseMessagingService : FirebaseMessagingService(), KoinComponent {

    val fcm: FcmCommon by inject()
    val appInfo: AppInfo by inject()
    val json: Json by inject()

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        logger.d { "Received message: ${remoteMessage.data}" }
        val data = remoteMessage.data

        if (data.isEmpty()) return


        if (data["type"] == "BOLTZ_EVENT") {
            tryCatchNull {
                val boltzNotificationData = BoltzNotificationSimple.create(json, data)
                fcm.handleBoltzPushNotification(boltzNotificationData)
            }
        } else {
            val notificationType = data["notification_type"]

            // Breez Notification
            if (notificationType == "payment_received" || notificationType == "tx_confirmed" || notificationType == "address_txs_confirmed") {
                val xpubHashId = data["app_data"]
                val breezNotification = BreezNotification.fromString(data["notification_payload"])

                if (appInfo.isDevelopmentOrDebug) {
                    fcm.showDebugNotification(
                        title = "Notification Received", message = breezNotification.toString()
                    )
                }

                if (breezNotification != null && !xpubHashId.isNullOrBlank()) {
                    fcm.handleLightningPushNotification(xpubHashId, breezNotification)
                } else {
                    logger.d { "No notification_payload or app_data $data" }
                }
            } else {
                //we don't have any other notification types
                //so for now lets just show it as-is
                //add notification types for other types
                val notification = MeldNotificationData.create(remoteMessage.data)

                if (notification.type == MeldNotificationType.MELD_TRANSACTION) {
                    fcm.showBuyTransactionNotification(notification)
                }
            }
        }
    }

    override fun onNewToken(token: String) {
        logger.d { "Refreshed token: $token" }
        fcm.setToken(token)
    }

    companion object : Loggable()
}

