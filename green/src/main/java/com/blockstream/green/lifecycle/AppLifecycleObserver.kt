package com.blockstream.green.lifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import mu.KLogging


// Session handling moved to SessionManager
// Currently unused
class AppLifecycleObserver : LifecycleObserver {

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