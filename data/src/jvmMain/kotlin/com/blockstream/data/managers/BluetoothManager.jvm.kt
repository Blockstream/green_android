package com.blockstream.data.managers

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

actual open class BluetoothManager {
    actual val bluetoothState: StateFlow<BluetoothState>
        get() = MutableStateFlow(BluetoothState.UNAVAILABLE)

    actual fun permissionsGranted() {

    }
}