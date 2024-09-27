package com.blockstream.common.interfaces

import com.blockstream.common.devices.GreenDevice
import com.blockstream.common.gdk.device.GdkHardwareWallet
import com.blockstream.common.gdk.device.HardwareConnectInteraction
import com.blockstream.jade.HttpRequestHandler
import com.blockstream.jade.firmware.JadeFirmwareManager

data class ConnectionResult(
    val isJadeUninitialized: Boolean? = false
)

interface DeviceConnectionInterface {
    @Throws(Exception::class)
    suspend fun connectDevice(
        device: GreenDevice,
        httpRequestHandler: HttpRequestHandler,
        interaction: HardwareConnectInteraction
    ): ConnectionResult

    @Throws(Exception::class)
    suspend fun authenticateDeviceIfNeeded(
        httpRequestHandler: HttpRequestHandler,
        interaction: HardwareConnectInteraction,
        gdkHardwareWallet: GdkHardwareWallet,
        jadeFirmwareManager: JadeFirmwareManager? = null
    )
}