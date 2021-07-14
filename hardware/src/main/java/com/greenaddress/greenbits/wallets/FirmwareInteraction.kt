package com.greenaddress.greenbits.wallets;

import androidx.arch.core.util.Function
import com.blockstream.DeviceBrand

interface FirmwareInteraction {
    fun askForFirmwareUpgrade(
        deviceBrand: DeviceBrand,
        version: String?,
        isUpgradeRequired: Boolean,
        callback: Function<Boolean?, Void?>?
    )
}