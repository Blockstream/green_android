package com.blockstream.data.fcm

import com.blockstream.data.crypto.GreenKeystore
import com.blockstream.data.data.CredentialType
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.data.SwapType
import com.blockstream.data.database.Database
import com.blockstream.data.di.ApplicationScope
import com.blockstream.data.extensions.greenlightCredentials
import com.blockstream.data.extensions.isBtcToLightning
import com.blockstream.data.extensions.launchSafe
import com.blockstream.data.extensions.lightningMnemonic
import com.blockstream.data.extensions.logException
import com.blockstream.data.lightning.GreenlightSdk
import com.blockstream.data.managers.SessionManager
import com.blockstream.data.notifications.models.BoltzNotificationSimple
import com.blockstream.data.notifications.models.MeldNotificationData
import com.blockstream.utils.Loggable
import kotlinx.coroutines.delay
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class FcmCommon constructor(val applicationScope: ApplicationScope) : KoinComponent {
    private val database: Database by inject()
    private val greenKeystore: GreenKeystore by inject()
    private val sessionManager: SessionManager by inject()

    private var _token: String? = null

    val token
        get() = _token

    open fun setToken(token: String) {
        logger.d { "FCM Token: ${token}" }
        _token = token
    }

    abstract fun scheduleLightningBackgroundJob(
        walletId: String
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

    // TODO: currently unused. Rewire to a real settled-payment event to restore the "Payment received" notification.
    abstract suspend fun showLightningPaymentNotification(
        wallet: GreenWallet,
        paymentHash: String,
        satoshi: Long
    )

    abstract suspend fun showLightningBackgroundNotification(
        wallet: GreenWallet
    )

    abstract suspend fun showOpenWalletNotification(
        wallet: GreenWallet
    )

    abstract fun showDebugNotification(
        title: String,
        message: String,
    )

    abstract fun showBuyTransactionNotification(
        meldNotificationData: MeldNotificationData
    )

    protected suspend fun wallet(walletId: String) = database.getWallet(walletId)

    suspend fun doLightningBackgroundWork(walletId: String) {
        logger.d { "doLightningBackgroundWork for walletId:$walletId" }

        val existingSession = sessionManager.getWalletSessionOrNull(walletId)
        if (existingSession?.isConnected == true && existingSession.lightningSdkOrNull != null) {
            logger.d { "Signer already attached via live session, skipping background wake for walletId:$walletId" }
            return
        }

        wallet(walletId)?.also { wallet ->
            val mnemonic = database.getLoginCredential(
                id = wallet.id,
                credentialType = CredentialType.KEYSTORE_LIGHTNING_MNEMONIC
            )?.lightningMnemonic(greenKeystore = greenKeystore)

            val credentials = database.getLoginCredentials(wallet.id).greenlightCredentials
                ?.greenlightCredentials(greenKeystore = greenKeystore)

            if (mnemonic == null || credentials == null) {
                logger.d { "Couldn't decrypt mnemonic/credentials for walletId:$walletId" }
                return@also
            }

            // Start only the signer so the cloud node can settle the in-flight payment, then stop it.
            val handle = GreenlightSdk.startSigner(mnemonic = mnemonic, credentials = credentials)

            try {
                delay(SIGNER_WAKE_DURATION_MS)
            } finally {
                handle.stop()
            }

            logger.d { "doLightningBackgroundWork completed walletId:$walletId" }
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

                // Boltz is about to pay an invoice generated by our own Greenlight node, which can
                // only be settled while the signer is up. Liquid funded submarine swaps don't need
                // this, the destination there is not our node.
                if (status == "invoice.pending" && swap.isBtcToLightning) {
                    logger.d { "Waking Lightning signer for BTC->LN swap ${notification.id}" }
                    scheduleLightningBackgroundJob(walletId = wallet.id)
                }
            }

            scheduleBoltzBackgroundJob(notification)
        }
    }

    fun handleGreenlightPushNotification(xpubHashId: String) {
        applicationScope.launchSafe(context = logException()) {
            val wallet = database.getMainnetWalletWithXpubHashId(xpubHashId) ?: run {
                logger.d { "Greenlight: wallet not found for $xpubHashId" }
                return@launchSafe
            }

            val hasLightningMnemonic = database.getLoginCredentials(wallet.id).lightningMnemonic?.encrypted_data != null

            // Every event must wake the signer so the cloud node can settle in-flight operations.
            if (hasLightningMnemonic) {
                scheduleLightningBackgroundJob(wallet.id)
                showLightningBackgroundNotification(wallet = wallet)
            } else {
                logger.d { "Greenlight: no lightning mnemonic for ${wallet.id}" }
                showOpenWalletNotification(wallet)
            }
        }
    }

    companion object : Loggable() {
        private const val SIGNER_WAKE_DURATION_MS = 60_000L
    }
}
