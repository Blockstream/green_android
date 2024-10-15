package com.blockstream.green.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.blockstream.common.managers.SessionManager
import com.blockstream.green.managers.NotificationManagerAndroid
import com.blockstream.common.utils.Loggable
import org.koin.android.ext.android.inject

class TaskService : Service() {

    private val notificationManager: NotificationManagerAndroid by inject()

    private val sessionManager: SessionManager by inject()

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

    companion object : Loggable()
}