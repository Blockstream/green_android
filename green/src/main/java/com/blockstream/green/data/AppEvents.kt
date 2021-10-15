package com.blockstream.green.data

interface AppEvent

sealed class GdkEvent: AppEvent{
    object Success : GdkEvent()
}

sealed class NavigateEvent: AppEvent{
    object Navigate : NavigateEvent()
    data class NavigateBack(val reason: Throwable? = null) : NavigateEvent()
    data class NavigateWithData(val data: Any? = null): NavigateEvent()
}