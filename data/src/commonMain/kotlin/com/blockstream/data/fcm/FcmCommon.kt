package com.blockstream.data.fcm

import breez_sdk.BreezEvent
import com.blockstream.data.config.AppInfo
import com.blockstream.data.crypto.GreenKeystore
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.data.SwapType
import com.blockstream.data.database.Database
import com.blockstream.data.di.ApplicationScope
import com.blockstream.data.extensions.launchSafe
import com.blockstream.data.extensions.lightningMnemonic
import com.blockstream.data.extensions.logException
import com.blockstream.data.lightning.BreezNotification
import com.blockstream.data.lightning.satoshi
import com.blockstream.data.managers.SessionManager
import com.blockstream.data.notifications.models.BoltzNotificationSimple
import com.blockstream.data.notifications.models.MeldNotificationData
import com.blockstream.data.utils.randomChars
import com.blockstream.utils.Loggable
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class FcmCommon constructor(val applicationScope: ApplicationScope) : KoinComponent {
    private val database: Database by inject()
    private val greenKeystore: GreenKeystore by inject()
    private val sessionManager: SessionManager by inject()

    private val appInfo: AppInfo by inject()

    private var _token: String? = null

    val token
        get() = _token

    open fun setToken(token: String) {
        logger.d { "FCM Token: ${token}" }
        _token = token
    }

    abstract fun scheduleLightningBackgroundJob(
        walletId: String,
        breezNotification: BreezNotification
    )

    abstract fun scheduleBoltzBackgroundJob(
        boltzNotificationData: BoltzNotificationSimple
    )

    abstract suspend fun showSwapPaymentReceivedNotification(
        wallet: GreenWallet
    )

    abstract suspend fun showSwapPaymentSentNotification(
        wallet: GreenWallet
    )

    abstract suspend fun showSwapNotification(
        wallet: GreenWallet
    )

    abstract suspend fun showLightningPaymentNotification(
        wallet: GreenWallet,
        paymentHash: String,
        satoshi: Long
    )

    abstract suspend fun showOpenWalletNotification(
        wallet: GreenWallet,
        breezNotification: BreezNotification
    )

    abstract fun showDebugNotification(
        title: String,
        message: String,
    )

    abstract fun showBuyTransactionNotification(
        meldNotificationData: MeldNotificationData
    )

    protected suspend fun wallet(walletId: String) = database.getWallet(walletId)

    suspend fun doLightningBackgroundWork(walletId: String, breezNotification: BreezNotification) {
        logger.d { "doLightningBackgroundWork for walletId:$walletId with data: $breezNotification" }

        if (appInfo.isDevelopmentOrDebug) {
            showDebugNotification(
                title = "Background Work",
                message = breezNotification.toString()
            )
        }

        wallet(walletId)?.also { wallet ->
            database.getLoginCredentials(wallet.id).lightningMnemonic?.encrypted_data?.let {
                greenKeystore.decryptData(it).decodeToString()
            }?.also { mnemonic ->
                sessionManager.getLightningBridge(mnemonic, wallet.isTestnet).also {
                    it.connectToGreenlight(mnemonic = mnemonic)

                    // Wait maximum 2 minutes to complete all operations
                    val success = withTimeoutOrNull(120_000) {

                        if (appInfo.isDevelopmentOrDebug) {
                            showDebugNotification(
                                title = "Lightning connected and waiting",
                                message = breezNotification.toString()
                            )
                        }

                        if (breezNotification.paymentHash == "test") {
                            showLightningPaymentNotification(
                                wallet = wallet,
                                paymentHash = randomChars(10),
                                satoshi = 999_999_000L + (111..999).random()
                            )
                        } else {
                            it.eventSharedFlow.filter { event ->
                                event is BreezEvent.InvoicePaid
                            }.firstOrNull()?.also {
                                (it as? BreezEvent.InvoicePaid)?.also {
                                    showLightningPaymentNotification(
                                        wallet = wallet,
                                        paymentHash = it.details.paymentHash,
                                        satoshi = it.details.payment?.amountMsat?.satoshi() ?: 0
                                    )
                                }
                            }
                        }

                        it.eventSharedFlow.filter { event ->
                            event is BreezEvent.Synced
                        }.firstOrNull()
                    }

                    if (appInfo.isDevelopmentOrDebug) {
                        showDebugNotification(
                            title = "Lightning disconnected: Success: $success",
                            message = breezNotification.toString()
                        )
                    }

                    logger.d { "doLightningBackgroundWork completed walletId:$walletId" }

                    it.release()
                }
            } ?: logger.d { "Couldn't decrypt mnemonic" }
        } ?: run {
            logger.d { "Wallet not found $walletId" }
        }
    }

    fun handleBoltzPushNotification(notification: BoltzNotificationSimple) {
        applicationScope.launchSafe {

            val swap = database.getSwap(id = notification.id)
            val wallet =
                swap?.let { database.getWallet(id = it.wallet_id) ?: database.getMainnetWalletWithXpubHashId(xPubHashId = it.xpub_hash_id) }

            if (wallet != null) {
                val status = notification.status

                val isSwapComplete = when {
                    swap.swap_type == SwapType.NormalSubmarine && status == "invoice.paid" -> true
                    swap.swap_type == SwapType.ReverseSubmarine && status == "invoice.settled" -> true
                    swap.swap_type == SwapType.Chain && status == "transaction.claimed" -> true
                    else -> false
                }

                if (isSwapComplete) {
                    when {
                        !swap.is_auto_swap -> showSwapNotification(wallet = wallet) // It's user initiated swap
                        swap.swap_type == SwapType.NormalSubmarine -> {
                            showSwapPaymentSentNotification(wallet = wallet)
                        }

                        swap.swap_type == SwapType.ReverseSubmarine -> {
                            showSwapPaymentReceivedNotification(wallet = wallet)
                        }
                    }
                }
            }

            scheduleBoltzBackgroundJob(notification)
        }
    }

    fun handleLightningPushNotification(xpubHashId: String, breezNotification: BreezNotification) {
        applicationScope.launchSafe(context = logException()) {
            database.getMainnetWalletWithXpubHashId(xpubHashId)?.also { wallet ->

                val mnemonic =
                    database.getLoginCredentials(wallet.id).lightningMnemonic?.encrypted_data?.let {
                        greenKeystore.decryptData(it).decodeToString()
                    }

                if (mnemonic != null) {
                    logger.d { "scheduleBackgroundJob" }
                    scheduleLightningBackgroundJob(wallet.id, breezNotification)
                } else {
                    logger.d { "showNotification" }
                    showOpenWalletNotification(wallet, breezNotification)
                }
            } ?: run {
                logger.d { "wallet not found" }
            }
        }
    }

    companion object : Loggable()
}
