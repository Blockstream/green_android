package com.blockstream.common.devices

import android.content.Context
import android.hardware.usb.UsbDevice
import com.blockstream.common.utils.Loggable
import com.juul.kable.Peripheral

/*
 * If a BLE device, DeviceManager will update peripheral if required due to RPA, it's safe to consider
 * Device to be updated with latest broadcasted Peripheral
 */
class JadeUsbDevice constructor(
    context: Context,
    deviceManager: DeviceManagerAndroid,
    usbDevice: UsbDevice? = null,
    peripheral: Peripheral? = null,
    isBonded: Boolean = false
) : AndroidDevice(
    context = context,
    deviceManager = deviceManager,
    usbDevice = usbDevice,
    deviceBrand = DeviceBrand.Blockstream,
    type = ConnectionType.USB,
    peripheral = peripheral,
    isBonded = isBonded
), JadeDeviceApi by JadeDeviceApiImpl(), JadeDevice {

    companion object : Loggable() {
        const val VENDOR_JADE_A = 0x10c4
        const val VENDOR_JADE_B = 0x1a86

        private fun hasSuportedVendorId(usbDevice: UsbDevice): Boolean {
            val vId = usbDevice.vendorId
            return (vId == VENDOR_JADE_A ||
                    vId == VENDOR_JADE_B)
        }

        fun fromUsbDevice(deviceManager: DeviceManagerAndroid, usbDevice: UsbDevice): JadeUsbDevice? {
            if (hasSuportedVendorId(usbDevice)) {
                return JadeUsbDevice(
                    context = deviceManager.context,
                    deviceManager = deviceManager,
                    usbDevice = usbDevice
                )
            }
            return null
        }
    }
}