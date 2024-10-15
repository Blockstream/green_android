package com.blockstream.common.managers

import kotlinx.coroutines.flow.StateFlow

actual open class BluetoothManager {
    actual val bluetoothState: StateFlow<BluetoothState>
        get() = TODO("Not yet implemented")

    actual fun permissionsGranted() {
    }
}