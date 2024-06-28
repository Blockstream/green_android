package com.blockstream.green.devices

import com.blockstream.HwWalletLogin
import com.blockstream.common.devices.DeviceBrand
import com.blockstream.common.devices.GreenDevice
import com.blockstream.common.gdk.device.HardwareWalletInteraction
import com.greenaddress.greenbits.wallets.FirmwareInteraction

interface HardwareConnectInteraction : FirmwareInteraction, HwWalletLogin,
    HardwareWalletInteraction {
    fun showInstructions(resId: Int)
    fun showError(err: String)

    fun onDeviceReady(device: GreenDevice, isJadeUninitialized: Boolean?)
    fun onDeviceFailed(device: GreenDevice)

    fun requestPinBlocking(deviceBrand: DeviceBrand): String
}