package com.blockstream.common.devices

enum class DeviceBrand(val brand: String) {
    Blockstream("Blockstream"), Ledger("Ledger"), Trezor("Trezor");

    val isTrezor
        get() = this == Trezor

    val isLedger
        get() = this == Ledger

    val isJade
        get() = this == Blockstream

    val hasBleConnectivity
        get() = this != Trezor
}