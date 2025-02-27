package com.blockstream.common.devices

import android.app.Activity
import java.lang.ref.WeakReference

// In Android module
class AndroidActivityProvider : ActivityProvider {
    private var weakActivity: WeakReference<Activity>? = null

    fun setActivity(activity: Activity) {
        weakActivity = WeakReference(activity)
    }

    override fun getCurrentActivity(): Activity? {
        return weakActivity?.get()
    }

    fun clearActivity() {
        weakActivity = null
    }
}