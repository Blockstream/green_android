package com.blockstream.data.extensions

import com.juul.kable.PeripheralBuilder
import com.juul.kable.PlatformAdvertisement
import com.juul.kable.ScannerBuilder

actual fun ScannerBuilder.conflate() {
    preConflate = true
}

actual fun PeripheralBuilder.setupJade(mtu: Int) {
    onServicesDiscovered {
        requestMtu(mtu)
    }
}

actual fun PlatformAdvertisement.isBonded(): Boolean {
    return bondState == PlatformAdvertisement.BondState.Bonded
}