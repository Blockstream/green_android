package com.blockstream.green.managers

import android.content.Context
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.di.ApplicationScope
import com.blockstream.common.fcm.FcmCommon
import com.blockstream.common.lightning.BreezNotification
import com.blockstream.common.utils.Loggable
import com.blockstream.green.work.LightningWork
import org.koin.core.component.inject

class FcmAndroid constructor(
    private val context: Context,
    applicationScope: ApplicationScope,
) : FcmCommon(applicationScope = applicationScope) {

    private val notificationManager: NotificationManager by inject()

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

    companion object : Loggable()
}