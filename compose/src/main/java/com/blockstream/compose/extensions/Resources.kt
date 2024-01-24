package com.blockstream.compose.extensions

import com.blockstream.common.data.WalletIcon
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.device.DeviceInterface
import com.blockstream.compose.R

fun WalletIcon.resource() = when(this) {
    WalletIcon.WATCH_ONLY -> R.drawable.eye
    WalletIcon.TESTNET -> R.drawable.flask
    WalletIcon.BIP39 -> R.drawable.wallet_passphrase
    WalletIcon.HARDWARE -> R.drawable.wallet_hw
    WalletIcon.LIGHTNING -> R.drawable.lightning_fill
    else -> R.drawable.wallet
}

fun DeviceInterface.icon(): Int{
    return when {
        deviceBrand.isTrezor -> R.drawable.trezor_device
        deviceBrand.isLedger -> R.drawable.ledger_device
        else -> R.drawable.blockstream_jade_device
    }
}

fun String.getNetworkIcon(): Int{
    if (Network.isBitcoinMainnet(this)) return R.drawable.bitcoin
    if (Network.isLiquidMainnet(this)) return R.drawable.liquid
    if (Network.isBitcoinTestnet(this)) return R.drawable.bitcoin_testnet
    if (Network.isLiquidTestnet(this)) return R.drawable.liquid_testnet
    if (Network.isLightningMainnet(this)) return R.drawable.bitcoin_lightning
    return R.drawable.unknown
}

fun Network.icon(): Int = network.getNetworkIcon()