package com.blockstream.compose.events

import com.blockstream.compose.sideeffects.SideEffect

interface EventWithSideEffect : Event {
    val sideEffect: SideEffect
}