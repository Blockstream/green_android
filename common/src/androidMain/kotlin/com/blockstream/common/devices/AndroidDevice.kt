package com.blockstream.common.devices

import android.content.Context
import android.hardware.usb.UsbDevice
import com.juul.kable.Peripheral

/*
 * If a BLE device, DeviceManager will update peripheral if required due to RPA, it's safe to consider
 * Device to be updated with latest broadcasted Peripheral
 */
abstract class AndroidDevice constructor(
    val context: Context,
    val deviceManager: DeviceManagerAndroid,
    val usbDevice: UsbDevice? = null,
    type: ConnectionType,
    deviceBrand: DeviceBrand,
    peripheral: Peripheral? = null,
    isBonded: Boolean = false,
) : GreenDeviceImpl(deviceBrand = deviceBrand, type = type, peripheral = peripheral, isBonded = isBonded) {

    val usbManager
        get() = deviceManager.usbManager

    override fun askForUsbPermission(onSuccess: (() -> Unit), onError: ((throwable: Throwable?) -> Unit)?) {
        usbDevice?.let {
            deviceManager.askForUsbPermissions(device = it, onError = onError, onSuccess = onSuccess)
        } ?: run {
            onSuccess()
        }
    }

    override val connectionIdentifier: String by lazy {
        usbDevice?.deviceId?.toString(10) ?: super.connectionIdentifier
    }

    override val uniqueIdentifier: String
        get() = try {
            (if (isUsb) usbDevice?.serialNumber else null) ?: super.uniqueIdentifier
        } catch (e: Exception) {
            e.printStackTrace()
            super.uniqueIdentifier
        }

    override val name
        get() = (if (deviceBrand.isJade && isUsb) "Jade" else usbDevice?.productName) ?: super.name

    // Jade v1 has the controller manufacturer as a productName
    override val manufacturer
        get() = if (deviceBrand.isJade) deviceBrand.name else usbDevice?.productName ?: super.manufacturer

    override fun hasPermissions(): Boolean {
        return usbDevice?.let { deviceManager.hasPermissions(it) } ?: super.hasPermissions()
    }
}

fun GreenDevice.toAndroidDevice() = this as? AndroidDevice