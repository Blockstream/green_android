package com.blockstream.jade

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.blockstream.jade.connection.JadeUsbConnection

fun JadeInterface.Companion.fromUsb(
    usbDevice: UsbDevice,
    usbManager: UsbManager
): JadeInterface {
    return JadeInterface(
        JadeUsbConnection(
            usbDevice = usbDevice,
            usbManager = usbManager
        )
    )
}