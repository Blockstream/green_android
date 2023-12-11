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
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.LogoutReason
import com.blockstream.common.database.Database
import com.blockstream.common.di.ApplicationScope
import com.blockstream.common.extensions.logException
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.managers.SessionManager
import com.blockstream.common.managers.SettingsManager
import com.blockstream.green.BuildConfig
import com.blockstream.green.R
import com.blockstream.green.data.CountlyAndroid
import com.blockstream.green.gdk.getNetworkColor
import com.blockstream.green.gdk.getWallet
import com.blockstream.green.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import mu.KLogging

class NotificationManager constructor(
    private val context: Context,
    private val applicationScope: ApplicationScope,
    private val androidNotificationManager: NotificationManager,
    private val sessionManager: SessionManager,
    private val settingsManager: SettingsManager,
    private val database: Database,
    private val countly: CountlyAndroid
) : DefaultLifecycleObserver {
    private var scope: CoroutineScope = CoroutineScope(SupervisorJob())

    private var isOnForeground: Boolean = false

    private var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_LOGOUT) {
                intent.extras?.getString(WALLET_ID)?.also { walletId ->
                    applicationScope.launch(context = logException(countly)) {
                        sessionManager.getWalletSessionOrNull(walletId)?.disconnectAsync(reason = LogoutReason.USER_ACTION)
                    }
                }
            }
        }
    }

    init {
        // Listen to foreground / background events
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        createNotificationChannels()

        sessionManager.connectionChangeEvent.onEach {
            updateNotifications()
        }.launchIn(scope)

        ContextCompat.registerReceiver(
            context,
            broadcastReceiver,
            IntentFilter(ACTION_LOGOUT),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    override fun onResume(owner: LifecycleOwner) {
        isOnForeground = true
        updateNotifications()
    }

    override fun onPause(owner: LifecycleOwner) {
        isOnForeground = false
        updateNotifications()
    }

    fun cancelAll() {
        androidNotificationManager.cancelAll()
    }

    fun notificationPermissionGiven() {
        updateNotifications()
    }

    private fun updateNotifications() {

        // do a for list to avoid Concurrent modification exception
        // TODO fix
        sessionManager.getSessions().toList().forEach { session ->

            applicationScope.launch(context = logException(countly)) {

                session.getWallet(database, sessionManager)?.also { wallet ->
                    if (session.isConnected) {
                        val sessionTimeout =
                            if (isOnForeground) 0 else (session.getSettings(null)?.altimeout
                                ?: 1) * 60 * 1000L

                        val notification = createWalletNotification(
                            context = context,
                            session = session,
                            wallet = wallet,
                            timeout = sessionTimeout
                        )
                        androidNotificationManager.notify(notificationId(wallet), notification)
                    } else {
                        androidNotificationManager.cancel(notificationId(wallet))
                    }
                }
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            if (androidNotificationManager.getNotificationChannel(WALLETS_CHANNEL_ID) == null) {
                androidNotificationManager.createNotificationChannel(
                    NotificationChannel(
                        WALLETS_CHANNEL_ID,
                        context.getString(R.string.id_connected_wallets),
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
            }
        }
    }

    private fun createWalletNotification(
        context: Context,
        session: GdkSession,
        wallet: GreenWallet,
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
            .setColor(
                ContextCompat.getColor(
                    context,
                    if (session.gdkSessions.size == 1) session.mainAssetNetwork.id.getNetworkColor() else R.color.brand_green
                )
            )
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(R.drawable.ic_close, context.getString(R.string.id_logout), logoutIntent)
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

                setVisibility(if (settingsManager.appSettings.enhancedPrivacy) NotificationCompat.VISIBILITY_SECRET else NotificationCompat.VISIBILITY_PRIVATE)
            }
            .build()
    }

    private fun notificationId(wallet: GreenWallet): Int = wallet.id.hashCode()

    // Intents are cached by the requestCode, in order for wallet to be updated we have to provide a unique requestCode
    private fun requestCode(wallet: GreenWallet): Int = wallet.hashCode()

    companion object : KLogging() {
        const val WALLETS_CHANNEL_ID = "${BuildConfig.APPLICATION_ID}.WALLETS_CHANNEL_ID"

        const val ACTION_LOGOUT = "${BuildConfig.APPLICATION_ID}.ACTION_LOGOUT"
        const val WALLET_ID = "WALLET_ID"
    }
}