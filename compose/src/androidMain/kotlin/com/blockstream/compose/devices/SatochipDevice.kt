package com.blockstream.compose.devices

import android.content.Context
import android.hardware.usb.UsbDevice
import com.blockstream.common.devices.ActivityProvider
import com.blockstream.common.devices.AndroidDevice
import com.blockstream.common.devices.ConnectionType
import com.blockstream.common.devices.DeviceBrand
import com.blockstream.common.devices.DeviceManagerAndroid
import com.blockstream.common.devices.GreenDevice
import com.blockstream.common.devices.NfcDevice
import com.blockstream.common.devices.NfcDeviceType
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

        fun fromNfcDevice(
            deviceManager: DeviceManagerAndroid,
            nfcDevice: NfcDevice,
            activityProvider: ActivityProvider?,
        ): SatochipDevice? {
            if (nfcDevice.type == NfcDeviceType.SATOCHIP) {
                return SatochipDevice(
                    context = deviceManager.context,
                    deviceManager = deviceManager,
                    type = ConnectionType.NFC,
                    usbDevice = null,
                    activityProvider = activityProvider,
                )
            } else {
                return null
            }
        }


    }
}