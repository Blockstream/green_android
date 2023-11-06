package com.blockstream.compose.extensions

import com.blockstream.common.data.WalletIcon
import com.blockstream.compose.R

fun WalletIcon.resource() = when(this) {
    WalletIcon.WATCH_ONLY -> R.drawable.eye
    WalletIcon.TESTNET -> R.drawable.flask
    WalletIcon.BIP39 -> R.drawable.wallet_passphrase
    WalletIcon.HARDWARE -> R.drawable.wallet_hw
    WalletIcon.LIGHTNING -> R.drawable.lightning_fill
    else -> R.drawable.wallet
}
