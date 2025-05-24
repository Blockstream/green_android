package com.blockstream.compose.components

import androidx.compose.runtime.Composable

enum class RiveAnimation {
    NONE, ROCKET, LIGHTNING_TRANSACTION, ACCOUNT_ARCHIVED, CHECKMARK, WALLET, RECOVERY_PHRASE,
    GREEN_TO_BLOCKSTREAM, CREATE_WALLET, JADE_BUTTON, JADE_SCROLL, JADE_POWER, JADE_UPDATE, JADE_PLUS_BUTTON,
    JADE_PLUS_SCROLL, JADE_PLUS_POWER, JADE_PLUS_UPDATE;
}

@Composable
expect fun Rive(riveAnimation: RiveAnimation)