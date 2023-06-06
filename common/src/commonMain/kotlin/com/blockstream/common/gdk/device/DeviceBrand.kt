package com.blockstream.common.gdk.device

enum class DeviceBrand {
    Blockstream, Ledger, Trezor;

    val isTrezor
        get() = this == Trezor

    val isLedger
        get() = this == Ledger

    val isJade
        get() = this == Blockstream

    val brand
        get() = when (this) {
            Blockstream -> "Blockstream"
            Ledger -> "Ledger"
            Trezor -> "Trezor"
        }

    val hasBleConnectivity
        get() = this != Trezor
}