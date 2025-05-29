package com.blockstream.common.managers

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.blockstream.green.utils.Loggable
import com.juul.tuulbox.coroutines.flow.broadcastReceiverFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

private fun Context.getLocationManagerOrNull() =
    ContextCompat.getSystemService(this, LocationManager::class.java)

private fun Context.isLocationEnabledOrNull(): Boolean? =
    getLocationManagerOrNull()?.let(LocationManagerCompat::isLocationEnabled)

actual class BluetoothManager constructor(val context: Context, val bluetoothAdapter: BluetoothAdapter?) {
    private val scope = CoroutineScope(context = Dispatchers.Default)

    private val blePermissionsUpdate = MutableSharedFlow<Unit>(replay = 1).also {
        // Set init value
        it.tryEmit(Unit)
    }

    private fun getBluetoothManagerOrNull(): android.bluetooth.BluetoothManager? =
        ContextCompat.getSystemService(context, android.bluetooth.BluetoothManager::class.java)

    private fun getBluetoothAdapterOrNull(): BluetoothAdapter? =
        getBluetoothManagerOrNull()?.adapter

    private val locationEnabledOrNullFlow = when {
        VERSION.SDK_INT > VERSION_CODES.R -> flowOf(true)
        else -> broadcastReceiverFlow(IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION), ContextCompat.RECEIVER_NOT_EXPORTED)
            .map { intent ->
                if (VERSION.SDK_INT == VERSION_CODES.R) {
                    intent.getBooleanExtra(LocationManager.EXTRA_PROVIDER_ENABLED, false)
                } else {
                    context.isLocationEnabledOrNull()
                }
            }
            .onStart { emit(context.isLocationEnabledOrNull()) }
            .distinctUntilChanged()
    }

    private val _bluetoothState: Flow<BluetoothState> = flow {
        when (val adapter = getBluetoothAdapterOrNull()) {
            null -> emit(BluetoothState.UNAVAILABLE)
            else -> emitAll(
                broadcastReceiverFlow(IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
                    .map { intent -> intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR) }
                    .onStart {
                        emit(if (adapter.isEnabled) BluetoothAdapter.STATE_ON else BluetoothAdapter.STATE_OFF)
                    }
                    .map { state ->
                        logger.d { "Bluetooth state changed to $state" }
                        when (state) {
                            BluetoothAdapter.STATE_ON -> BluetoothState.ON
                            BluetoothAdapter.STATE_OFF -> BluetoothState.OFF
                            BluetoothAdapter.STATE_TURNING_OFF -> BluetoothState.UNAVAILABLE
                            BluetoothAdapter.STATE_TURNING_ON -> BluetoothState.UNAVAILABLE
                            else -> error("Unexpected bluetooth state: $state")
                        }
                    },
            )
        }
    }

    private val hasPermissions
        get() = BLE_PERMISSIONS.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    actual fun permissionsGranted() {
        blePermissionsUpdate.tryEmit(Unit)
    }

    actual val bluetoothState: StateFlow<BluetoothState> =
        (combine(
            locationEnabledOrNullFlow,
            blePermissionsUpdate,
            _bluetoothState,
        ) { locationEnabled, _, bluetoothState ->
            when (locationEnabled) {
                true -> if (hasPermissions) bluetoothState else BluetoothState.PERMISSIONS_NOT_GRANTED
                false -> BluetoothState.LOCATION_SERVICES_DISABLED
                null -> BluetoothState.UNAVAILABLE
            }
        }).stateIn(scope, SharingStarted.Eagerly, BluetoothState.UNAVAILABLE)

    companion object : Loggable() {

        // NOTE: BLE_LOCATION_PERMISSION should be set to FINE for Android 10 and above, or COARSE for 9 and below
        // See: https://developer.android.com/about/versions/10/privacy/changes#location-telephony-bluetooth-wifi
        val BLE_PERMISSIONS = when {
            VERSION.SDK_INT > VERSION_CODES.R -> listOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )

            VERSION.SDK_INT > VERSION_CODES.P -> listOf(Manifest.permission.ACCESS_FINE_LOCATION)
            else -> listOf(Manifest.permission.ACCESS_COARSE_LOCATION)
        }.toTypedArray()
    }
}