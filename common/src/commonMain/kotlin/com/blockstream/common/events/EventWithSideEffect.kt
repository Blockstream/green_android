package com.blockstream.common.events

import com.blockstream.common.sideeffects.SideEffect

interface EventWithSideEffect : Event {
    val sideEffect: SideEffect
}