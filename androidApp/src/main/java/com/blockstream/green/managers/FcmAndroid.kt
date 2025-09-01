package com.blockstream.green.managers

import android.content.Context
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.di.ApplicationScope
import com.blockstream.common.fcm.FcmCommon
import com.blockstream.common.lightning.BreezNotification
import com.blockstream.green.data.notifications.models.NotificationData
import com.blockstream.green.utils.Loggable
import com.blockstream.green.work.LightningWork
import com.blockstream.green.work.MeldPendingTransactionsWorker
import org.koin.core.component.inject

class FcmAndroid constructor(
    private val context: Context,
    applicationScope: ApplicationScope,
) : FcmCommon(applicationScope = applicationScope) {

    private val notificationManager: NotificationManagerAndroid by inject()

    override fun scheduleLightningBackgroundJob(
        walletId: String,
        breezNotification: BreezNotification
    ) {
        LightningWork.create(walletId, breezNotification, context)
    }

    override suspend fun showOpenWalletNotification(
        wallet: GreenWallet,
        breezNotification: BreezNotification
    ) {
        logger.d { "showNotification $wallet" }
        notificationManager.createOpenWalletNotification(context, wallet)
    }

    override suspend fun showLightningPaymentNotification(
        wallet: GreenWallet,
        paymentHash: String,
        satoshi: Long,
    ) {
        logger.d { "showPaymentNotification $wallet" }
        notificationManager.createPaymentNotification(context, wallet, paymentHash, satoshi)
    }

    override fun showDebugNotification(
        title: String,
        message: String,
    ) {
        notificationManager.createDebugNotification(
            context = context,
            title = title,
            message = message
        )
    }

    fun scheduleMeldBackgroundJob(
        externalCustomerId: String
    ) {
        MeldPendingTransactionsWorker.trigger(context, externalCustomerId)
    }

    override fun showBuyTransactionNotification(
        notificationData: NotificationData
    ) {
        notificationManager.createBuyTransactionNotification(context, notificationData)
        
        notificationData.payload?.externalCustomerId?.let { externalCustomerId ->
            scheduleMeldBackgroundJob(externalCustomerId)
        }
    }

    companion object : Loggable()
}