package com.blockstream.jade.firmware;

import kotlinx.coroutines.Deferred

interface FirmwareInteraction: HardwareQATester {
    fun askForFirmwareUpgrade(firmwareUpgradeRequest: FirmwareUpgradeRequest): Deferred<Int?>

    fun firmwareUpdateState(state: FirmwareUpdateState)

//    fun firmwarePushedToDevice(firmwareFileData: FirmwareFileData, hash: String)
//    fun firmwareProgress(written: Int, totalSize: Int)
//    fun firmwareFailed(userCancelled: Boolean, error: String, firmwareFileData: FirmwareFileData)
//    fun firmwareComplete(success: Boolean, firmwareFileData: FirmwareFileData)
//    fun firmwareUpdated(requireReconnection: Boolean, requireBleRebonding: Boolean)
}

data class FirmwareUpgradeRequest(
    val deviceBrand: String,
    val isUsb: Boolean,
    val currentVersion: String?,
    val upgradeVersion: String?,
    val firmwareList: List<String>?,
    val hardwareVersion: String?,
    val isUpgradeRequired: Boolean
)