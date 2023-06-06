package com.blockstream.common.managers

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LifecycleState{
    Foreground, Background;

    fun isForeground(): Boolean {
        return this == Foreground
    }

    fun isBackground(): Boolean {
        return this == Background
    }
}

class LifecycleManager {
    private var isOnForeground: Boolean = false

    private val _lifecycleState = MutableStateFlow(LifecycleState.Background)
    val lifecycleState = _lifecycleState.asStateFlow()

    fun setOnForeground(isOnForeground: Boolean){
        _lifecycleState.value = if(isOnForeground) LifecycleState.Foreground else LifecycleState.Background
    }
}