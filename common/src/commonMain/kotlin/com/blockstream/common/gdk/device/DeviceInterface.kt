package com.blockstream.common.gdk.device

import kotlinx.coroutines.flow.StateFlow

enum class DeviceState {
    SCANNED, DISCONNECTED
}

interface DeviceInterface {
    val id: String
    val name: String
    val deviceBrand: DeviceBrand
    val isUsb: Boolean
    val isBle: Boolean
    val isOffline: Boolean
    val gdkHardwareWallet: GdkHardwareWallet?

    val isJade: Boolean
    val isTrezor: Boolean
    val isLedger: Boolean

    val deviceState : StateFlow<DeviceState>
    fun disconnect()
}