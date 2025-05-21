package com.blockstream.compose.components

import androidx.compose.runtime.Composable

enum class RiveAnimation {
    NONE, ROCKET, LIGHTNING_TRANSACTION, ACCOUNT_ARCHIVED, CHECKMARK, WALLET, JADE_BUTTON, JADE_SCROLL, JADE_POWER, RECOVERY_PHRASE, GREEN_TO_BLOCKSTREAM;
}

@Composable
expect fun Rive(riveAnimation: RiveAnimation)