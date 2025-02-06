package com.blockstream.common.devices

enum class DeviceBrand(val brand: String) {
    Blockstream("Blockstream"), Ledger("Ledger"), Trezor("Trezor"), Satochip("Satochip"), Generic("Generic");

    val isTrezor
        get() = this == Trezor

    val isLedger
        get() = this == Ledger

    val isJade
        get() = this == Blockstream

    // SATODEBUG
    val isSatochip
        get() = this == Satochip

    val isGeneric
        get() = this == Generic

    val hasBleConnectivity
        get() = this != Trezor
}