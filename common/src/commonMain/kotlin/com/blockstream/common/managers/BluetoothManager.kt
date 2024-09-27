package com.blockstream.common.managers

import kotlinx.coroutines.flow.StateFlow

enum class BluetoothState {
    OFF, UNAVAILABLE, ADAPTER_NOT_AVAILABLE, PERMISSIONS_NOT_GRANTED, LOCATION_SERVICES_DISABLED, ON
}

expect class BluetoothManager {
    val bluetoothState: StateFlow<BluetoothState>
}