package com.blockstream.common.managers

import kotlinx.coroutines.flow.StateFlow

public sealed class BluetoothState {
    data object Off : BluetoothState()
    data object Unavailable: BluetoothState()
    data object AdapterNotAvailable: BluetoothState()
    data object PermissionsNotGranted : BluetoothState()
    data object LocationServicesDisabled : BluetoothState()
    data object On : BluetoothState()
}

expect class BluetoothManager {
    val bluetoothState: StateFlow<BluetoothState>
}