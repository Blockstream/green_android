package com.blockstream.common.fcm

import breez_sdk.BreezEvent
import com.blockstream.common.crypto.GreenKeystore
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.database.Database
import com.blockstream.common.di.ApplicationScope
import com.blockstream.common.extensions.lightningMnemonic
import com.blockstream.common.extensions.logException
import com.blockstream.common.lightning.BreezNotification
import com.blockstream.common.lightning.satoshi
import com.blockstream.common.managers.SessionManager
import com.blockstream.common.utils.Loggable
import com.blockstream.common.utils.randomChars
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesIgnore
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class FcmCommon(val applicationScope: ApplicationScope) : KoinComponent {
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
        walletId: String,
        breezNotification: BreezNotification
    )

    @NativeCoroutinesIgnore
    abstract suspend fun showLightningPaymentNotification(
        wallet: GreenWallet,
        paymentHash: String,
        satoshi: Long
    )

    @NativeCoroutinesIgnore
    abstract suspend fun showOpenWalletNotification(
        wallet: GreenWallet,
        breezNotification: BreezNotification
    )

    @NativeCoroutinesIgnore
    protected suspend fun wallet(walletId: String) = database.getWallet(walletId)

    @NativeCoroutinesIgnore
    suspend fun doLightningBackgroundWork(walletId: String, breezNotification: BreezNotification) {
        logger.d { "doLightningBackgroundWork for walletId:$walletId with data: $breezNotification" }
        wallet(walletId)?.also { wallet ->
            database.getLoginCredentials(wallet.id).lightningMnemonic?.encrypted_data?.let {
                greenKeystore.decryptData(it).decodeToString()
            }?.also { mnemonic ->
                sessionManager.getLightningBridge(mnemonic, wallet.isTestnet).also {
                    it.connectToGreenlight(mnemonic = mnemonic)

                    // Wait maximum 2 minutes to complete all operations
                    withTimeoutOrNull(120_000) {

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

                    it.release()
                }
            } ?: logger.d { "Couldn't decrypt mnemonic" }
        } ?: run {
            logger.d { "Wallet not found $walletId" }
        }
    }

    fun handleLightningPushNotification(xpubHashId: String, breezNotification: BreezNotification) {
        applicationScope.launch(context = logException()) {
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