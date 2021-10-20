package com.greenaddress.greenbits.wallets;

import androidx.arch.core.util.Function
import com.blockstream.DeviceBrand

interface FirmwareInteraction {
    fun askForFirmwareUpgrade(
        firmwareUpgradeRequest: FirmwareUpgradeRequest,
        callback: Function<Boolean?, Void?>?
    )
}

data class FirmwareUpgradeRequest(
    val deviceBrand: DeviceBrand,
    val isUsb: Boolean,
    val currentVersion: String?,
    val upgradeVersion: String?,
    val hardwareVersion: String?,
    val isUpgradeRequired: Boolean
)