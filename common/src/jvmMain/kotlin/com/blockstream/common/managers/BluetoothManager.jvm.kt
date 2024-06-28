package com.blockstream.common.managers

import kotlinx.coroutines.flow.StateFlow

actual class BluetoothManager {
    actual val bluetoothState: StateFlow<BluetoothState>
        get() = TODO("Not yet implemented")
}