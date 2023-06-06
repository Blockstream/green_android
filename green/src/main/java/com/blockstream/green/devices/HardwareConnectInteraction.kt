package com.blockstream.green.devices

import com.blockstream.HwWalletLogin
import com.blockstream.common.gdk.device.DeviceBrand
import com.blockstream.common.gdk.device.HardwareWalletInteraction
import com.greenaddress.greenbits.wallets.FirmwareInteraction

interface HardwareConnectInteraction : FirmwareInteraction, HwWalletLogin,
    HardwareWalletInteraction {
    fun showInstructions(resId: Int)
    fun showError(err: String)

    fun onDeviceReady(device: Device, isJadeUninitialized: Boolean?)
    fun onDeviceFailed(device: Device)

    fun requestPinBlocking(deviceBrand: DeviceBrand): String
}