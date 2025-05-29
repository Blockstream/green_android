package com.blockstream.common.sideeffects

import com.blockstream.ui.events.Event
import com.blockstream.ui.sideeffects.SideEffect

interface SideEffectWithEvent : SideEffect {
    val event: Event
}