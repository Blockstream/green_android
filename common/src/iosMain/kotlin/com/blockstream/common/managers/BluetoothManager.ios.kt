package com.blockstream.common.managers

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

actual open class BluetoothManager {
    actual val bluetoothState: StateFlow<BluetoothState> = MutableStateFlow(BluetoothState.ON)
    actual fun permissionsGranted() {
    }
}