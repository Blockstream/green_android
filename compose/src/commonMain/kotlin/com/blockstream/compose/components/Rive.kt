package com.blockstream.compose.components

import androidx.compose.runtime.Composable

enum class RiveAnimation {
    ROCKET,
    LIGHTNING_TRANSACTION, LIGHTNING_SUCCESS,
    ACCOUNT_ARCHIVED, CHECKMARK, WALLET, RECOVERY_PHRASE,
    GREEN_TO_BLOCKSTREAM, CREATE_WALLET, JADE_BUTTON, JADE_SCROLL, JADE_POWER, JADE_UPDATE, JADE_PLUS_BUTTON,
    JADE_PLUS_SCROLL, JADE_PLUS_POWER, JADE_PLUS_UPDATE;
}

// Path of the animation's .riv file in the Compose resources
val RiveAnimation.filePath: String
    get() = when (this) {
        RiveAnimation.LIGHTNING_TRANSACTION -> "lightning_transaction.riv"
        RiveAnimation.ROCKET -> "rocket.riv"
        RiveAnimation.ACCOUNT_ARCHIVED -> "account_archived.riv"
        RiveAnimation.CHECKMARK -> "checkmark.riv"
        RiveAnimation.WALLET -> "wallet.riv"
        RiveAnimation.RECOVERY_PHRASE -> "recovery_phrase.riv"
        RiveAnimation.GREEN_TO_BLOCKSTREAM -> "green_to_blockstream.riv"
        RiveAnimation.CREATE_WALLET -> "create_wallet.riv"
        RiveAnimation.JADE_BUTTON -> "jade_button.riv"
        RiveAnimation.JADE_SCROLL -> "jade_scroll.riv"
        RiveAnimation.JADE_POWER -> "jade_power.riv"
        RiveAnimation.JADE_UPDATE -> "jade_power.riv"
        RiveAnimation.JADE_PLUS_BUTTON -> "jade_plus_button.riv"
        RiveAnimation.JADE_PLUS_SCROLL -> "jade_plus_scroll.riv"
        RiveAnimation.JADE_PLUS_POWER -> "jade_plus_power.riv"
        RiveAnimation.JADE_PLUS_UPDATE -> "jade_plus_update.riv"
        RiveAnimation.LIGHTNING_SUCCESS -> "lightning_success.riv"
    }.let {
        "files/rive/$it"
    }

@Composable
expect fun Rive(riveAnimation: RiveAnimation, previewContent: @Composable (() -> Unit)? = null)