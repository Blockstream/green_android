package com.blockstream.common.managers

import com.blockstream.common.utils.Loggable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single

enum class LifecycleState{
    Foreground, Background;

    fun isForeground(): Boolean {
        return this == Foreground
    }

    fun isBackground(): Boolean {
        return this == Background
    }
}

@Single
class LifecycleManager {
    private val _lifecycleState = MutableStateFlow(LifecycleState.Background)
    val lifecycleState = _lifecycleState.asStateFlow()

    fun updateState(isOnForeground: Boolean){
        logger.d { "updateState (isOnForeground:$isOnForeground)" }
        _lifecycleState.value = if(isOnForeground) LifecycleState.Foreground else LifecycleState.Background
    }

    companion object: Loggable()
}