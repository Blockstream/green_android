package com.blockstream.common.utils

import com.blockstream.common.managers.SettingsManager

fun generateWalletName(settingsManager: SettingsManager): String {
    return "My Wallet ${settingsManager.walletCounter().plus(1).takeIf { it > 1 } ?: ""}".trim()
}