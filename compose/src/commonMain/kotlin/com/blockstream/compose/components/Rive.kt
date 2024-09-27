package com.blockstream.compose.components

import androidx.compose.runtime.Composable

enum class RiveAnimation {
    ROCKET, LIGHTNING_TRANSACTION, ACCOUNT_ARCHIVED, CHECKMARK, WALLET, JADE_BUTTON, JADE_SCROLL, JADE_POWER, RECOVERY_PHRASE;
}

@Composable
expect fun Rive(riveAnimation: RiveAnimation)