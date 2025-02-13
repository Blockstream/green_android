package com.blockstream.common.events

import com.blockstream.ui.events.Event
import com.blockstream.ui.sideeffects.SideEffect

interface EventWithSideEffect : Event {
    val sideEffect: SideEffect
}