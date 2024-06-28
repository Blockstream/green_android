package com.blockstream

import com.blockstream.common.devices.DeviceBrand
import com.blockstream.hardware.R
import kotlinx.coroutines.flow.MutableStateFlow

fun DeviceBrand.icon(): Int = when (this) {
    DeviceBrand.Blockstream -> R.drawable.ic_blockstream
    DeviceBrand.Ledger -> R.drawable.ic_ledger
    DeviceBrand.Trezor -> R.drawable.ic_trezor
}


fun DeviceBrand.deviceIcon(): Int = when (this) {
    DeviceBrand.Blockstream -> R.drawable.blockstream_jade_device
    DeviceBrand.Ledger -> R.drawable.ledger_device
    DeviceBrand.Trezor -> R.drawable.trezor_device
}

fun createDisconnectEvent(): MutableStateFlow<Boolean> {
    return MutableStateFlow(false)
}