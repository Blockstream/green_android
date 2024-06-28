package com.blockstream.green.devices

import android.content.Context
import android.hardware.usb.UsbDevice
import android.os.ParcelUuid
import com.blockstream.common.devices.ConnectionType
import com.blockstream.common.devices.DeviceBrand
import com.blockstream.common.devices.GreenDevice
import com.blockstream.common.utils.Loggable
import com.btchip.comm.LedgerDeviceBLE
import com.juul.kable.Peripheral
import kotlinx.datetime.Clock

/*
 * If a BLE device, DeviceManager will update peripheral if required due to RPA, it's safe to consider
 * Device to be updated with latest broadcasted Peripheral
 */
class AndroidDevice constructor(
    val context: Context,
    val deviceManager: DeviceManagerAndroid,
    val usbDevice: UsbDevice? = null,
    type: ConnectionType,
    deviceBrand: DeviceBrand,
    peripheral: Peripheral? = null,
    isBonded: Boolean = false,
) : GreenDevice(deviceBrand = deviceBrand, type = type, peripheral = peripheral, isBonded = isBonded) {

    val usbManager
        get() = deviceManager.usbManager

    fun askForPermission(onSuccess: (() -> Unit), onError: ((throwable: Throwable?) -> Unit)? = null) {
        usbDevice?.let {
            deviceManager.askForPermissions(device = it, onError = onError, onSuccess = onSuccess)
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

    fun needsUsbPermissionsToIdentify(): Boolean {
        return isUsb && !hasPermissions()
    }

    companion object : Loggable() {
        const val VENDOR_BTCHIP = 0x2581
        const val VENDOR_LEDGER = 0x2c97
        const val VENDOR_TREZOR = 0x534c
        const val VENDOR_TREZOR_V2 = 0x1209
        const val VENDOR_JADE_A = 0x10c4
        const val VENDOR_JADE_B = 0x1a86

        private fun hasSuportedVendorId(usbDevice: UsbDevice) : Boolean{
            val vId = usbDevice.vendorId
            return (vId == VENDOR_BTCHIP ||
                    vId == VENDOR_LEDGER ||
                    vId == VENDOR_TREZOR ||
                    vId == VENDOR_TREZOR_V2 ||
                    vId == VENDOR_JADE_A ||
                    vId == VENDOR_JADE_B)
        }

        private fun deviceBrand(usbDevice: UsbDevice? = null, bleService: ParcelUuid? = null): DeviceBrand {
            return when {
                (usbDevice?.vendorId == VENDOR_TREZOR || usbDevice?.vendorId == VENDOR_TREZOR_V2) -> DeviceBrand.Trezor
                (bleService == ParcelUuid(LedgerDeviceBLE.SERVICE_UUID) || usbDevice?.vendorId == VENDOR_BTCHIP || usbDevice?.vendorId == VENDOR_LEDGER) -> DeviceBrand.Ledger
                else -> DeviceBrand.Blockstream
            }
        }

        fun fromUsbDevice(deviceManager: DeviceManagerAndroid, usbDevice: UsbDevice): AndroidDevice? {
            if(hasSuportedVendorId(usbDevice)){
                return AndroidDevice(
                    context = deviceManager.context,
                    deviceManager = deviceManager,
                    type = ConnectionType.USB,
                    usbDevice = usbDevice,
                    deviceBrand = deviceBrand(usbDevice = usbDevice)
                )
            }
            return null
        }

        fun fromScan(
            deviceManager: DeviceManagerAndroid,
            bleService: ParcelUuid?,
            peripheral: Peripheral? = null,
            isBonded: Boolean
        ): AndroidDevice {
            return AndroidDevice(
                context = deviceManager.context,
                deviceManager = deviceManager,
                type = ConnectionType.BLUETOOTH,
                peripheral = peripheral,
                isBonded = isBonded,
                deviceBrand = deviceBrand(bleService = bleService)
            ).also {
                it.timeout = Clock.System.now().toEpochMilliseconds()
            }
        }
    }
}

fun GreenDevice.toAndroidDevice() = this as? AndroidDevice