package com.blockstream.green.lifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.blockstream.green.gdk.SessionManager
import mu.KLogging


// TODO ADD session timeout handling
// For now session timeout is handled by the v3 codebase
class AppLifecycleObserver(val sessionManager: SessionManager) : LifecycleObserver {

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onEnterForeground() {
        logger().debug { "onEnterForeground" }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onEnterBackground() {
        logger().debug { "onEnterBackground" }
    }

    companion object: KLogging()
}