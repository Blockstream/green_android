package com.blockstream.common.extensions

import com.blockstream.common.managers.DeviceManager.Companion.JADE
import com.juul.kable.PeripheralBuilder
import com.juul.kable.PlatformAdvertisement
import com.juul.kable.ScannerBuilder

expect fun ScannerBuilder.conflate()

expect fun PeripheralBuilder.setupJade(mtu: Int)

expect fun PlatformAdvertisement.isBonded(): Boolean

val PlatformAdvertisement.isJade
    get() = uuids.firstOrNull()?.toString() == JADE