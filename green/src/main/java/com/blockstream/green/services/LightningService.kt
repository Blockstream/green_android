package com.blockstream.green.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.blockstream.green.managers.NotificationManager
import com.blockstream.green.settings.SettingsManager
import com.blockstream.lightning.LightningManager
import dagger.hilt.android.AndroidEntryPoint
import mu.KLogging
import java.util.Timer
import javax.inject.Inject
import kotlin.concurrent.schedule
import kotlin.time.Duration.Companion.minutes

@Deprecated("Do not use this, it's inefficient")
@AndroidEntryPoint
class LightningService : Service() {

    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var notificationManager: NotificationManager

    @Inject
    lateinit var lightningManager: LightningManager

    private var keepAliveTimer: Timer? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logger.info { "onStartCommand" }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(
                NotificationManager.LIGHTNING_NOTIFICATION_ID.hashCode(),
                notificationManager.createLightningNotification(applicationContext)
            )
        } else {
            notificationManager.showLightningNotification()
        }

        keepAliveTimer?.cancel()
        keepAliveTimer = Timer().also {
            it.schedule(15.minutes.inWholeMilliseconds) {
                stopSelf()
            }
        }

        lightningManager.setKeepAlive(true)

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        logger.info { "onDestroy" }
        super.onDestroy()
        lightningManager.setKeepAlive(false)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            notificationManager.hideLightningNotification()
        }
    }

    companion object : KLogging() {
        fun start(context: Context) {
            Intent(context, LightningService::class.java).also {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(it)
                } else {
                    context.startService(it)
                }
            }
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, LightningService::class.java))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}