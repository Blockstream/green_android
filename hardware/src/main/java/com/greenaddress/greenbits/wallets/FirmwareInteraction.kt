package com.greenaddress.greenbits.wallets;

import com.blockstream.common.gdk.device.DeviceBrand
import com.blockstream.HardwareQATester
import kotlinx.coroutines.Deferred

interface FirmwareInteraction: HardwareQATester {
    fun askForFirmwareUpgrade(
        firmwareUpgradeRequest: FirmwareUpgradeRequest
    ): Deferred<Int?>

    fun firmwarePushedToDevice(firmwareFileData: FirmwareFileData, hash: String)

    fun firmwareProgress(written: Int, totalSize: Int)

    fun firmwareFailed(userCancelled: Boolean, error: String, firmwareFileData: FirmwareFileData)

    fun firmwareComplete(success: Boolean, firmwareFileData: FirmwareFileData)

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