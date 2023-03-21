package com.blockstream.green.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.blockstream.green.managers.NotificationManager
import com.blockstream.green.managers.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import mu.KLogging
import javax.inject.Inject

@AndroidEntryPoint
class TaskService : Service() {

    @Inject
    lateinit var notificationManager: NotificationManager

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Important, do not restart service
        return START_NOT_STICKY
    }

    // App removed from Task Manager
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        sessionManager.disconnectAll()
        notificationManager.cancelAll()
    }

    companion object : KLogging()
}