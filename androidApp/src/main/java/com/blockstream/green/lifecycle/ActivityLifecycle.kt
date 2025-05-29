package com.blockstream.green.lifecycle

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.LifecycleObserver
import com.blockstream.common.managers.SessionManager
import com.blockstream.green.GreenActivity
import com.blockstream.green.managers.NotificationManagerAndroid
import com.blockstream.green.utils.Loggable

class ActivityLifecycle(
    private val sessionManager: SessionManager,
    private val notificationManager: NotificationManagerAndroid
) : Application.ActivityLifecycleCallbacks, LifecycleObserver {

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {

    }

    override fun onActivityStarted(activity: Activity) {

    }

    override fun onActivityResumed(activity: Activity) {

    }

    override fun onActivityPaused(activity: Activity) {

    }

    override fun onActivityStopped(activity: Activity) {

    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {

    }

    override fun onActivityDestroyed(activity: Activity) {
        logger.i { "onActivityDestroyed" }
        if (activity is GreenActivity) {
            sessionManager.disconnectAll()
            notificationManager.cancelAll()
        }
    }

    companion object : Loggable()
}