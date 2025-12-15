package com.blockstream.data.devices

import com.blockstream.utils.Loggable
import com.juul.kable.Peripheral

/*
 * If a BLE device, DeviceManager will update peripheral if required due to RPA, it's safe to consider
 * Device to be updated with latest broadcasted Peripheral
 */
class JadeBleDevice constructor(
    peripheral: Peripheral? = null,
    isBonded: Boolean = false,
) : GreenDeviceImpl(
    deviceBrand = DeviceBrand.Blockstream,
    type = ConnectionType.BLUETOOTH,
    peripheral = peripheral,
    isBonded = isBonded
), JadeDeviceApi by JadeDeviceApiImpl(), JadeDevice {

    companion object : Loggable() {
        fun fromScan(
            peripheral: Peripheral? = null,
            isBonded: Boolean
        ): JadeBleDevice = JadeBleDevice(
            peripheral = peripheral,
            isBonded = isBonded
        )
    }
}