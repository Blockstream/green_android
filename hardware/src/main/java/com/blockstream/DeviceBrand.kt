package com.blockstream

import com.blockstream.hardware.R

enum class DeviceBrand {
    Blockstream, Ledger, Trezor;

    val brand
        get() = when (this) {
            Blockstream -> "Blockstream"
            Ledger -> "Ledger"
            Trezor -> "Trezor"
        }

    val icon
        get() = when (this) {
            Blockstream -> R.drawable.ic_blockstream
            Ledger -> R.drawable.ic_ledger
            Trezor -> R.drawable.ic_trezor
        }

    val hasBleConnectivity
        get() = this != Trezor
}