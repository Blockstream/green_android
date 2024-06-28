package com.blockstream.common.managers

import com.benasher44.uuid.uuidFrom
import com.blockstream.common.devices.GreenDevice
import com.blockstream.common.di.ApplicationScope
import com.blockstream.common.extensions.cancelChildren
import com.blockstream.common.extensions.childScope
import com.blockstream.common.extensions.conflate
import com.blockstream.common.extensions.isBonded
import com.blockstream.common.extensions.isJade
import com.blockstream.common.extensions.setupJade
import com.blockstream.common.utils.Loggable
import com.juul.kable.Filter
import com.juul.kable.PlatformAdvertisement
import com.juul.kable.Scanner
import com.juul.kable.logs.Logging
import com.juul.kable.logs.SystemLogEngine
import com.juul.kable.peripheral
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock


sealed class ScanStatus {
    data object Stopped : ScanStatus()
    data object Started : ScanStatus()
    data object Scanning : ScanStatus()
    data class Failed(val message: CharSequence) : ScanStatus()
}

open class DeviceManager constructor(
    val scope: ApplicationScope,
    val sessionManager: SessionManager,
    private val bluetoothManager: BluetoothManager,
    private val supportedBleDevices: List<String>
) {
    protected val usbDevices = MutableStateFlow<List<GreenDevice>>(listOf())
    protected val bleDevices = MutableStateFlow<List<GreenDevice>>(listOf())

    private val _status = MutableStateFlow<ScanStatus>(ScanStatus.Stopped)
    val status = _status.asStateFlow()

    val bluetoothState = bluetoothManager.bluetoothState

    private var scanScope: CoroutineScope = CoroutineScope(scope.coroutineContext + SupervisorJob() + Dispatchers.IO).childScope()

    val devices = combine(usbDevices, bleDevices) { ble, usb ->
        ble + usb
    }.stateIn(scope, SharingStarted.WhileSubscribed(), listOf())

    init {
        bluetoothManager.bluetoothState.onEach {
            logger.d { "Bluetooth state: $it , status = ${status.value}" }
            if (it == BluetoothState.On && status.value == ScanStatus.Started) {
                startBluetoothScanning()
            } else if (it != BluetoothState.On && status.value == ScanStatus.Scanning) {
                logger.d { "Pausing BLE scanning state: $it" }
                stopBluetoothScanning(CancellationException("pause"))
            }

            // Clear all devices
            if(it != BluetoothState.On){
                bleDevices.value = listOf()
            }
        }.launchIn(scope)
    }

    fun getDevice(deviceId: String?): GreenDevice? {
        return devices.value.find { it.connectionIdentifier == deviceId }
    }

    fun startBluetoothScanning() {
        if (_status.value == ScanStatus.Scanning) return // Scan already in progress.

        if(bluetoothManager.bluetoothState.value != BluetoothState.On){
            _status.value = ScanStatus.Started
            return
        }

        _status.value = ScanStatus.Scanning

        logger.i { "Start BLE scanning" }

        addBleConnectedDevices()

        scanScope.launch {
            do {
                delay(1000L)
                val ts = Clock.System.now().toEpochMilliseconds() - 3_000
                bleDevices.value = bleDevices.value.filter {
                    !it.isOffline && (it.timeout == 0L || it.timeout > ts)
                }
            } while (isActive)
        }

        val scanner = Scanner {
            // new filters DSL does not work as expected
//            filters {
//                match {
//                    services = supportedBleDevices.map {
//                        uuidFrom(it)
//                    }
//                }
//            }
            filters = supportedBleDevices.map {
                Filter.Service(uuidFrom(it))
            }
            conflate()
            logging {
                engine = SystemLogEngine
                level = Logging.Level.Data
                format = Logging.Format.Multiline
            }
        }

        scanner
            .advertisements
            .catch { cause ->
                _status.value = ScanStatus.Failed(cause.message ?: "Unknown error")
            }
            .onCompletion { cause ->
                _status.value = if ((cause as? CancellationException)?.message.equals("pause"))
                    ScanStatus.Started
                else if (cause == null || (cause is CancellationException))
                    ScanStatus.Stopped
                else _status.value
            }
            .onEach {
                advertisedDevice(it)
            }
            .launchIn(scanScope)
    }

    open fun advertisedDevice(advertisement: PlatformAdvertisement) {
        val isJade = advertisement.isJade

        if (isJade) {
            val peripheral = scope.peripheral(advertisement) {
                setupJade(JADE_MTU)
            }

            val device = GreenDevice.jadeFromScan(
                peripheral = peripheral,
                isBonded = advertisement.isBonded()
            )

            addBluetoothDevice(device)
        }
    }

    protected fun addBluetoothDevice(newDevice: GreenDevice) {
        bleDevices.value.find { it.connectionIdentifier == newDevice.connectionIdentifier }
            ?.also { oldDevice ->
                newDevice.peripheral?.let {
                    oldDevice.updateFromScan(it)
                }
            } ?: run {
            // Add it if new
            bleDevices.value += newDevice
        }
    }

    fun stopBluetoothScanning(cause: CancellationException? = null) {
        if (status.value != ScanStatus.Stopped) {
            logger.i { "Stop BLE scanning" }
            scanScope.cancelChildren(cause = cause)
        }
    }

    private fun addBleConnectedDevices() {
        sessionManager.getConnectedDevices().filter { it.isBle }.forEach {
            addBluetoothDevice(it)
        }
    }

    companion object : Loggable() {
        const val JADE = "6e400001-b5a3-f393-e0a9-e50e24dcca9e"
        const val JADE_MTU = 517
    }
}