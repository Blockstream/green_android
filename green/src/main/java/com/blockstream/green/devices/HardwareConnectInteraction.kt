package com.blockstream.green.devices

import com.blockstream.DeviceBrand
import com.blockstream.HwWalletLogin
import com.greenaddress.greenapi.HWWalletBridge
import com.greenaddress.greenbits.wallets.FirmwareInteraction

interface HardwareConnectInteraction : FirmwareInteraction, HwWalletLogin, HWWalletBridge {
    fun showInstructions(resId: Int)
    fun showError(err: String)

    fun onDeviceReady(device: Device, isJadeUninitialized: Boolean?)
    fun onDeviceFailed(device: Device)

    fun requestPinBlocking(deviceBrand: DeviceBrand): String
}