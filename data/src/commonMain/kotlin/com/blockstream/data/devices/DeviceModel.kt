package com.blockstream.data.devices

import com.blockstream.data.devices.DeviceBrand.Blockstream
import kotlinx.serialization.Serializable

@Serializable
enum class DeviceModel(val deviceModel: String) {
    BlockstreamGeneric("Blockstream"),
    BlockstreamJade("Jade"),
    BlockstreamJadePlus("Jade Plus"),
    BlockstreamJadeCore("Jade Core"),
    TrezorGeneric("Trezor"),
    TrezorModelT("Trezor Model T"),
    TrezorModelOne("Trezor Model One"),
    LedgerGeneric("Ledger"),
    LedgerNanoS("Ledger Nano S"),
    LedgerNanoX("Ledger Nano X"),
    Generic("Generic Hardware Wallet");

    val deviceBrand: DeviceBrand
        get() = when (this) {
            BlockstreamGeneric, BlockstreamJade, BlockstreamJadePlus, BlockstreamJadeCore -> Blockstream
            TrezorGeneric, TrezorModelT, TrezorModelOne -> DeviceBrand.Trezor
            LedgerGeneric, LedgerNanoS, LedgerNanoX -> DeviceBrand.Ledger
            Generic -> DeviceBrand.Generic
        }

    val isJade: Boolean
        get() = when (this) {
            BlockstreamGeneric, BlockstreamJade, BlockstreamJadePlus, BlockstreamJadeCore -> true
            else -> false
        }

    val zendeskValue: String
        get() = when (this) {
            BlockstreamGeneric -> "jade"
            BlockstreamJade -> "jade_classic"
            BlockstreamJadePlus -> "jade_plus"
            BlockstreamJadeCore -> "jade_core"
            TrezorModelT -> "trezor_t"
            TrezorModelOne -> "trezor_one"
            LedgerNanoS -> "ledger_nano_s"
            LedgerNanoX -> "ledger_nano_x"
            TrezorGeneric -> "trezor"
            LedgerGeneric -> "ledger"
            Generic -> "generic"
        }
}