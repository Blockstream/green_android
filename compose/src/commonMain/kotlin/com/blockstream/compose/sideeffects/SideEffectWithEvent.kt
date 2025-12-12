package com.blockstream.compose.sideeffects

import com.blockstream.compose.events.Event

interface SideEffectWithEvent : SideEffect {
    val event: Event
}