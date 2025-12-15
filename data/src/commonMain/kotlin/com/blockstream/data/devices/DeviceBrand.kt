package com.blockstream.data.devices

enum class DeviceBrand(val brand: String) {
    Blockstream("Blockstream"), Ledger("Ledger"), Trezor("Trezor"), Generic("Generic");

    val isTrezor
        get() = this == Trezor

    val isLedger
        get() = this == Ledger

    val isJade
        get() = this == Blockstream

    val isGeneric
        get() = this == Generic

    val hasBleConnectivity
        get() = this != Trezor
}