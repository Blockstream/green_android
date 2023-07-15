package com.blockstream.green.lifecycle

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.LifecycleObserver
import com.blockstream.common.managers.SessionManager
import com.blockstream.green.managers.NotificationManager
import com.blockstream.green.ui.MainActivity
import mu.KLogging
import org.koin.core.annotation.Single


@Single
class ActivityLifecycle(
    private val sessionManager: SessionManager,
    private val notificationManager: NotificationManager
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
        logger.info { "onActivityDestroyed" }
        if (activity is MainActivity) {
            sessionManager.disconnectAll()
            notificationManager.cancelAll()
        }
    }

    companion object : KLogging()
}