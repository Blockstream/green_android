package com.blockstream.common.sideeffects

import com.blockstream.common.events.Event

interface SideEffectWithEvent: SideEffect {
    val event: Event
}