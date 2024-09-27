package com.blockstream.jade

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager

fun JadeAPI.Companion.fromUsb(
    usbDevice: UsbDevice,
    usbManager: UsbManager,
    httpRequestHandler: HttpRequestHandler
): JadeAPI {
    val jade = JadeInterface.fromUsb(
        usbDevice = usbDevice,
        usbManager = usbManager
    )
    return JadeAPI(jade, httpRequestHandler)
}