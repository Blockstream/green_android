package com.blockstream.green.managers

import android.content.Context
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.di.ApplicationScope
import com.blockstream.data.fcm.FcmCommon
import com.blockstream.data.lightning.BreezNotification
import com.blockstream.data.notifications.models.BoltzNotificationSimple
import com.blockstream.data.notifications.models.MeldNotificationData
import com.blockstream.green.work.BoltzWork
import com.blockstream.green.work.LightningWork
import com.blockstream.green.work.MeldPendingTransactionsWorker
import com.blockstream.utils.Loggable
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

    override fun scheduleBoltzBackgroundJob(
        boltzNotificationData: BoltzNotificationSimple
    ) {
        BoltzWork.create(boltzNotificationData, context)
    }

    override suspend fun showOpenWalletNotification(
        wallet: GreenWallet,
        breezNotification: BreezNotification
    ) {
        logger.d { "showNotification $wallet" }
        notificationManager.createOpenWalletNotification(context, wallet)
    }

    override suspend fun showSwapPaymentReceivedNotification(wallet: GreenWallet) {
        logger.d { "showSwapPaymentReceivedNotification $wallet" }
        notificationManager.showSwapPaymentReceivedNotification(context, wallet)
    }

    override suspend fun showSwapPaymentSentNotification(wallet: GreenWallet) {
        logger.d { "showSwapPaymentSentNotification $wallet" }
        notificationManager.showSwapPaymentSentNotification(context, wallet)
    }

    override suspend fun showSwapNotification(wallet: GreenWallet) {
        logger.d { "showSwapNotification $wallet" }
        notificationManager.showSwapNotification(context, wallet)
    }

    override suspend fun showLightningPaymentNotification(
        wallet: GreenWallet,
        paymentHash: String,
        satoshi: Long,
    ) {
        logger.d { "showPaymentNotification $wallet" }
        notificationManager.createLightningPaymentNotification(context, wallet, paymentHash, satoshi)
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
        meldNotificationData: MeldNotificationData
    ) {
        notificationManager.createBuyTransactionNotification(context, meldNotificationData)

        meldNotificationData.payload?.externalCustomerId?.let { externalCustomerId ->
            scheduleMeldBackgroundJob(externalCustomerId)
        }
    }

    companion object : Loggable()
}
