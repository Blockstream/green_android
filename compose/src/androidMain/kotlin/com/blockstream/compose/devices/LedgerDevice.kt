@file:OptIn(ExperimentalUuidApi::class)

package com.blockstream.compose.devices

import android.content.Context
import android.hardware.usb.UsbDevice
import com.blockstream.data.devices.AndroidDevice
import com.blockstream.data.devices.ConnectionType
import com.blockstream.data.devices.DeviceBrand
import com.blockstream.data.devices.DeviceManagerAndroid
import com.blockstream.data.devices.GreenDevice
import com.blockstream.data.gdk.Gdk
import com.blockstream.data.gdk.data.Network
import com.blockstream.data.gdk.device.HardwareConnectInteraction
import com.blockstream.utils.Loggable
import com.btchip.comm.BTChipTransport
import com.btchip.comm.LedgerDeviceBLE
import com.greenaddress.greenbits.wallets.BTChipHWWallet
import com.juul.kable.Peripheral
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class LedgerDevice constructor(
    context: Context,
    deviceManager: DeviceManagerAndroid,
    usbDevice: UsbDevice? = null,
    type: ConnectionType,
    peripheral: Peripheral? = null,
    isBonded: Boolean = false,
) : AndroidDevice(
    context = context,
    deviceManager = deviceManager,
    usbDevice = usbDevice,
    deviceBrand = DeviceBrand.Ledger,
    type = type,
    peripheral = peripheral,
    isBonded = isBonded
) {

    val transport: BTChipTransport? = null

    override suspend fun getOperatingNetworkForEnviroment(greenDevice: GreenDevice, gdk: Gdk, isTestnet: Boolean): Network? =
        (gdkHardwareWallet as? BTChipHWWallet)?.network?.takeIf { it.isTestnet == isTestnet }

    override suspend fun getOperatingNetwork(
        greenDevice: GreenDevice,
        gdk: Gdk,
        interaction: HardwareConnectInteraction
    ): Network? = (gdkHardwareWallet as? BTChipHWWallet)?.network

    companion object : Loggable() {
        const val VENDOR_BTCHIP = 0x2581
        const val VENDOR_LEDGER = 0x2c97

        private fun hasSuportedVendorId(usbDevice: UsbDevice): Boolean {
            val vId = usbDevice.vendorId
            return (vId == VENDOR_BTCHIP ||
                    vId == VENDOR_LEDGER)
        }

        fun fromUsbDevice(
            deviceManager: DeviceManagerAndroid,
            usbDevice: UsbDevice
        ): LedgerDevice? = if (hasSuportedVendorId(usbDevice)) {
            LedgerDevice(
                context = deviceManager.context,
                deviceManager = deviceManager,
                type = ConnectionType.USB,
                usbDevice = usbDevice
            )
        } else {
            null
        }

        fun fromScan(
            deviceManager: DeviceManagerAndroid,
            bleService: Uuid?,
            peripheral: Peripheral? = null,
            isBonded: Boolean
        ): LedgerDevice? = if (bleService.toString() == LedgerDeviceBLE.SERVICE_UUID.toString()) {
            LedgerDevice(
                context = deviceManager.context,
                deviceManager = deviceManager,
                type = ConnectionType.BLUETOOTH,
                peripheral = peripheral,
                isBonded = isBonded
            )
        } else null
    }
}