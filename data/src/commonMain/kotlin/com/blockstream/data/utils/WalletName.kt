package com.blockstream.data.utils

import com.blockstream.data.managers.SettingsManager

fun generateWalletName(settingsManager: SettingsManager): String {
    return "My Wallet ${settingsManager.walletCounter().plus(1).takeIf { it > 1 } ?: ""}".trim()
}