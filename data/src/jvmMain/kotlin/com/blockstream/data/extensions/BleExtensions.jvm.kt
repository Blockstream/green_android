package com.blockstream.data.extensions

import com.juul.kable.PeripheralBuilder
import com.juul.kable.PlatformAdvertisement
import com.juul.kable.ScannerBuilder

actual fun ScannerBuilder.conflate() {}
actual fun PeripheralBuilder.setupJade(mtu: Int) {
}

actual fun PlatformAdvertisement.isBonded(): Boolean {
    return true
}