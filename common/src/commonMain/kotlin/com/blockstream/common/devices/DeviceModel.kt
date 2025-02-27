package com.blockstream.common.devices

import com.blockstream.common.devices.DeviceBrand.Blockstream

enum class DeviceModel(val deviceModel: String) {
    BlockstreamGeneric("Blockstream"),
    BlockstreamJade("Jade"),
    BlockstreamJadePlus("Jade Plus"),
    TrezorGeneric("Trezor"),
    TrezorModelT("Trezor Model T"),
    TrezorModelOne("Trezor Model One"),
    LedgerGeneric("Ledger"),
    LedgerNanoS("Ledger Nano S"),
    LedgerNanoX("Ledger Nano X"),
    SatochipGeneric("Satochip"),
    Generic("Generic Hardware Wallet");

    val deviceBrand: DeviceBrand
        get() = when (this) {
            BlockstreamGeneric, BlockstreamJade, BlockstreamJadePlus -> Blockstream
            TrezorGeneric, TrezorModelT, TrezorModelOne -> DeviceBrand.Trezor
            LedgerGeneric, LedgerNanoS, LedgerNanoX -> DeviceBrand.Ledger
            Generic -> DeviceBrand.Generic
            SatochipGeneric -> DeviceBrand.Satochip
        }

    val isJade: Boolean
        get() = when (this) {
            BlockstreamGeneric, BlockstreamJade, BlockstreamJadePlus -> true
            else -> false
        }

    val zendeskValue: String
        get() = when (this) {
            BlockstreamGeneric, BlockstreamJade -> "jade"
            BlockstreamJadePlus -> "jade_plus"
            TrezorModelT -> "trezor_t"
            TrezorModelOne -> "trezor_one"
            LedgerNanoS -> "ledger_nano_s"
            LedgerNanoX -> "ledger_nano_x"
            TrezorGeneric -> "trezor"
            LedgerGeneric -> "ledger"
            SatochipGeneric -> "satochip"
            Generic -> "generic"
        }
}