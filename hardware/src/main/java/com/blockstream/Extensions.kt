package com.blockstream

import kotlinx.coroutines.flow.MutableStateFlow

fun createDisconnectEvent(): MutableStateFlow<Boolean> {
    return MutableStateFlow(false)
}