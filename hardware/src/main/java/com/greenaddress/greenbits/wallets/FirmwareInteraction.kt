package com.greenaddress.greenbits.wallets;

import androidx.arch.core.util.Function
import com.blockstream.DeviceBrand
import com.greenaddress.greenapi.HardwareQATester

interface FirmwareInteraction: HardwareQATester {
    fun askForFirmwareUpgrade(
        firmwareUpgradeRequest: FirmwareUpgradeRequest,
        callback: Function<Int?, Void>
    )

    fun firmwareUpdated(requireReconnection: Boolean, requireBleRebonding: Boolean)
}

data class FirmwareUpgradeRequest(
    val deviceBrand: DeviceBrand,
    val isUsb: Boolean,
    val currentVersion: String?,
    val upgradeVersion: String?,
    val firmwareList: List<String>?,
    val hardwareVersion: String?,
    val isUpgradeRequired: Boolean
)