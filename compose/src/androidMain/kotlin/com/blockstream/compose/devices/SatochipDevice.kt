package com.blockstream.compose.devices

import android.content.Context
import android.hardware.usb.UsbDevice
import com.blockstream.common.devices.ActivityProvider
import com.blockstream.common.devices.AndroidDevice
import com.blockstream.common.devices.ConnectionType
import com.blockstream.common.devices.DeviceBrand
import com.blockstream.common.devices.DeviceManagerAndroid
import com.blockstream.common.devices.GreenDevice
import com.blockstream.common.gdk.Gdk
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.device.HardwareConnectInteraction
import com.blockstream.common.utils.Loggable
import com.juul.kable.Peripheral

class SatochipDevice constructor(
    context: Context,
    deviceManager: DeviceManagerAndroid,
    usbDevice: UsbDevice? = null,
    type: ConnectionType,
    peripheral: Peripheral? = null,
    isBonded: Boolean = false,
    activityProvider: ActivityProvider? = null
) : AndroidDevice(
    context = context,
    deviceManager = deviceManager,
    usbDevice = usbDevice,
    deviceBrand = DeviceBrand.Satochip,
    type = type,
    peripheral = peripheral,
    isBonded = isBonded
) {

    val activityProvider: ActivityProvider? = activityProvider

    override suspend fun getOperatingNetworkForEnviroment(greenDevice: GreenDevice, gdk: Gdk, isTestnet: Boolean): Network =
        if (isTestnet) gdk.networks().testnetBitcoinElectrum else gdk.networks().bitcoinElectrum

    override suspend fun getOperatingNetwork(
        greenDevice: GreenDevice,
        gdk: Gdk,
        interaction: HardwareConnectInteraction
    ): Network? = interaction.requestNetwork()

    companion object : Loggable() {
        //const val VENDOR_TREZOR = 0x534c
        //const val VENDOR_TREZOR_V2 = 0x1209

        private fun hasSuportedVendorId(usbDevice: UsbDevice): Boolean {
            return false
//            val vId = usbDevice.vendorId
//            return (vId == VENDOR_TREZOR ||
//                    vId == VENDOR_TREZOR_V2)
        }

        // TODO remove
        fun fromUsbDevice(
            deviceManager: DeviceManagerAndroid,
            usbDevice: UsbDevice
        ): SatochipDevice? {
//            if (hasSuportedVendorId(usbDevice)) {
//                return TrezorDevice(
//                    context = deviceManager.context,
//                    deviceManager = deviceManager,
//                    type = ConnectionType.USB,
//                    usbDevice = usbDevice,
//                )
//            }
            return null
        }

        fun fromNfcDevice(
            deviceManager: DeviceManagerAndroid,
            activityProvider: ActivityProvider?,
            // TODO add params?
            // add AID?
        ): SatochipDevice? {
            // todo check AID
            return SatochipDevice(
                context = deviceManager.context,
                deviceManager = deviceManager,
                type = ConnectionType.NFC,
                usbDevice = null,
                activityProvider = activityProvider,
            )
            //return null
        }


    }
}