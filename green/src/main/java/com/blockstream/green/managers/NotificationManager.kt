package com.blockstream.green.managers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.blockstream.green.ApplicationScope
import com.blockstream.green.BuildConfig
import com.blockstream.green.R
import com.blockstream.green.data.Countly
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletId
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.gdk.GreenSession
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.gdk.getNetworkColor
import com.blockstream.green.settings.SettingsManager
import com.blockstream.green.ui.MainActivity
import com.blockstream.green.utils.logException
import kotlinx.coroutines.launch
import mu.KLogging

class NotificationManager constructor(
    private val context: Context,
    private val applicationScope: ApplicationScope,
    private val androidNotificationManager: NotificationManager,
    private val sessionManager: SessionManager,
    private val settingsManager: SettingsManager,
    private val walletRepository: WalletRepository,
    private val countly: Countly
) : DefaultLifecycleObserver {

    private var isOnForeground: Boolean = false

    private var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            intent.extras?.getLong(WALLET_ID, -1)?.let { walletId ->
                if (intent.action == ACTION_LOGOUT) {
                    applicationScope.launch(context = logException(countly)) {
                        sessionManager.getWalletSessionOrNull(walletId)?.disconnectAsync()
                    }
                }
            }
        }
    }

    init {
        // Listen to foreground / background events
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        createNotificationChannel()

        sessionManager.connectionChangeEvent.observeForever {
            updateNotifications(isOnForeground)
        }

        context.registerReceiver(broadcastReceiver, IntentFilter(ACTION_LOGOUT))
    }

    override fun onResume(owner: LifecycleOwner) {
        isOnForeground = true
        updateNotifications(true)
    }

    override fun onPause(owner: LifecycleOwner) {
        isOnForeground = false
        updateNotifications(false)
    }

    private fun updateNotifications(isForeground: Boolean) {

        // do a for list to avoid Concurrent modification exception
        // TODO fix
        sessionManager.getSessions().toList().forEach {

            applicationScope.launch(context = logException(countly)) {

                (it.ephemeralWallet ?: sessionManager.getWalletIdFromSession(it)
                    ?.let { walletId -> walletRepository.getWalletSuspend(walletId) })?.let { wallet ->
                    if (it.isConnected) {
                        val sessionTimeout =
                            if (isForeground) 0 else (it.getSettings()?.altimeout
                                ?: 1) * 60 * 1000L

                        val notification =
                            createNotification(context, it, wallet, sessionTimeout)
                        androidNotificationManager.notify(notificationId(wallet), notification)
                    } else {
                        androidNotificationManager.cancel(notificationId(wallet))
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                WALLETS_CHANNEL_ID,
                context.getString(R.string.id_connected_wallets),
                NotificationManager.IMPORTANCE_LOW
            )

            if (androidNotificationManager.getNotificationChannel(WALLETS_CHANNEL_ID) == null) {
                androidNotificationManager.createNotificationChannel(channel)
            }
        }
    }

    private fun createNotification(
        context: Context,
        session: GreenSession,
        wallet: Wallet,
        timeout: Long
    ): Notification {
        val intent = Intent(context, MainActivity::class.java).also {
            it.action = MainActivity.OPEN_WALLET
            it.putExtra(MainActivity.WALLET, wallet)
            session.device?.let { device ->
                it.putExtra(MainActivity.DEVICE_ID, device.id)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context, requestCode(wallet), intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val logoutIntent = PendingIntent.getBroadcast(
            context,
            requestCode(wallet),
            Intent(ACTION_LOGOUT).also {
                it.putExtra(WALLET_ID, wallet.id)
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, WALLETS_CHANNEL_ID)
            .setContentTitle(wallet.name)
            .setColorized(true)
            .setSmallIcon(R.drawable.ic_stat_green)
            .setContentIntent(pendingIntent)
            .setColor(ContextCompat.getColor(context, wallet.network.getNetworkColor()))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(
                R.drawable.ic_close,
                context.getString(R.string.id_logout),
                logoutIntent
            )
            .apply {

                session.device?.let {
                    setContentText(it.name)
                }

                if (timeout > 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        setWhen(System.currentTimeMillis() + timeout)
                        setShowWhen(true)
                        setUsesChronometer(true)
                        setChronometerCountDown(true)
                    }
                    // Automatically removed after timeout
                    setTimeoutAfter(timeout)
                } else {
                    setShowWhen(false)
                }

                setVisibility(if (settingsManager.getApplicationSettings().enhancedPrivacy) NotificationCompat.VISIBILITY_SECRET else NotificationCompat.VISIBILITY_PRIVATE)
            }
            .build()
    }

    // Make hardware wallet id positive
    private fun notificationId(wallet: Wallet): Int = notificationId(wallet.id)
    private fun notificationId(walletId: WalletId): Int = (10000 + walletId).toInt()

    // Intents are cached by the requestCode, in order for wallet to be updated we have to provide a unique requestCode
    private fun requestCode(wallet: Wallet): Int = wallet.hashCode()

    companion object : KLogging() {
        const val WALLETS_CHANNEL_ID = "${BuildConfig.APPLICATION_ID}.WALLETS_CHANNEL_ID"
        const val ACTION_LOGOUT = "${BuildConfig.APPLICATION_ID}.ACTION_LOGOUT"
        const val WALLET_ID = "WALLET_ID"
    }
}