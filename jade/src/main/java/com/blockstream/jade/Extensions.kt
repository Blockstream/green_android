package com.blockstream.jade

import kotlinx.coroutines.flow.MutableStateFlow

fun createDisconnectEvent(): MutableStateFlow<Boolean> {
    return MutableStateFlow(false)
}