package com.blockstream.common.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class Timer(
    delay: Long,
    dispatcher: CoroutineDispatcher = Dispatchers.Main,
    action: Timer.() -> Unit
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    init {
        scope.launch {
            delay(delay)

            if (this.isActive) {
                action.invoke(this@Timer)
            }
        }
    }

    fun cancel() {
        scope.cancel()
    }
}