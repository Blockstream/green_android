package com.blockstream.jade.firmware;

import kotlinx.coroutines.Deferred

interface FirmwareInteraction: HardwareQATester {
    fun askForFirmwareUpgrade(firmwareUpgradeRequest: FirmwareUpgradeRequest): Deferred<Int?>
    fun firmwareUpdateState(state: FirmwareUpdateState)
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