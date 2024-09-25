package com.blockstream.common.gdk.device

import com.blockstream.common.gdk.data.Account
import kotlinx.coroutines.flow.StateFlow

enum class DeviceState {
    SCANNED, DISCONNECTED
}

interface DeviceInterface {
    val connectionIdentifier: String
    val uniqueIdentifier: String
    val name: String
    val manufacturer: String?
    val deviceBrand: DeviceBrand
    val isUsb: Boolean
    val isBle: Boolean
    val isOffline: Boolean
    val gdkHardwareWallet: GdkHardwareWallet?

    val isJade: Boolean
    val isTrezor: Boolean
    val isLedger: Boolean

    val deviceState : StateFlow<DeviceState>

    fun canVerifyAddressOnDevice(account: Account): Boolean
    fun disconnect()
}