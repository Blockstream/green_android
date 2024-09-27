package com.blockstream.common.managers

import com.benasher44.uuid.uuidFrom
import com.blockstream.common.devices.DeviceState
import com.blockstream.common.devices.GreenDevice
import com.blockstream.common.devices.JadeBleDevice
import com.blockstream.common.di.ApplicationScope
import com.blockstream.common.extensions.cancelChildren
import com.blockstream.common.extensions.conflate
import com.blockstream.common.extensions.ifFalse
import com.blockstream.common.extensions.ifTrue
import com.blockstream.common.extensions.isBonded
import com.blockstream.common.extensions.isJade
import com.blockstream.common.extensions.setupJade
import com.blockstream.common.extensions.supervisorJob
import com.blockstream.common.utils.Loggable
import com.juul.kable.Filter
import com.juul.kable.PlatformAdvertisement
import com.juul.kable.Scanner
import com.juul.kable.logs.Logging
import com.juul.kable.logs.SystemLogEngine
import com.juul.kable.peripheral
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock


sealed class ScanStatus {
    data object Started : ScanStatus()
    data object Stopped : ScanStatus()
}

open class DeviceManager constructor(
    val scope: ApplicationScope,
    val sessionManager: SessionManager,
    private val bluetoothManager: BluetoothManager,
    private val supportedBleDevices: List<String>
) {
    private val deviceDiscovery = MutableStateFlow(false)

    protected val usbDevices = MutableStateFlow<List<GreenDevice>>(listOf())
    protected val bleDevices = MutableStateFlow<List<GreenDevice>>(listOf())

    private val _status = MutableStateFlow<ScanStatus>(ScanStatus.Stopped)
    val status = _status.asStateFlow()

    val bluetoothState = bluetoothManager.bluetoothState

    private var scanScope: CoroutineScope = scope.supervisorJob()

    private val disconnectEvent = bleDevices.flatMapLatest { devices ->
        combine(devices.map { it.deviceState }) { }
    }

    val devices = combine(usbDevices, bleDevices, disconnectEvent) { usb, ble, _ ->
        ble.filter { it.deviceState.value == DeviceState.CONNECTED } + usb
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    var savedDevice: GreenDevice? = null

    init {
        bluetoothManager.bluetoothState.onEach {
            logger.d { "Bluetooth state: $it , status = ${status.value}" }

            if (it == BluetoothState.ON && status.value == ScanStatus.Started) {
                startBluetoothScanning()
            } else if (it != BluetoothState.ON && status.value == ScanStatus.Started) {
                logger.d { "Pausing BLE scanning state: $it" }
                pauseBluetoothScanning()
            }

            // Clear all devices
            if(it != BluetoothState.ON){
                cleanupBleDevices(cleanAll = true)
            }
        }.launchIn(scope)

        deviceDiscovery.onEach {
            if (it) {
                startBluetoothScanning()
            } else {
                stopBluetoothScanning()
            }
        }.launchIn(scope)


    }

    fun getDevice(deviceId: String?): GreenDevice? {
        return devices.value.find { it.connectionIdentifier == deviceId }
            // Check if device is already in a session
            ?: sessionManager.getConnectedHardwareWalletSessions()
                .find { it.device?.connectionIdentifier == deviceId }?.device
            ?: savedDevice?.takeIf { it.connectionIdentifier == deviceId }
    }

    private fun cleanupBleDevices(cleanAll : Boolean = false) {
        val cleanupTs = Clock.System.now().toEpochMilliseconds() - 3_000
        bleDevices.value = if(cleanAll) listOf() else bleDevices.value.filter {
            it.isConnected && (it.heartbeat == 0L || it.heartbeat > cleanupTs)
        }
    }

    private fun startBluetoothScanning() {
        if(!deviceDiscovery.value) return // Device Discovery is off

        if(bluetoothManager.bluetoothState.value != BluetoothState.ON) return // Bluetooth is not enabled

        if (_status.value == ScanStatus.Started) return // Scan already in progress.

        _status.compareAndSet(ScanStatus.Stopped, ScanStatus.Started).ifFalse {
            return
        }

        logger.i { "Start BLE scanning" }

        addBleConnectedDevices()

        scanScope.launch {
            do {
                ensureActive()

                cleanupBleDevices()

                delay(1000L)
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
                cause.printStackTrace()
                stopBluetoothScanning()
            }
            .onCompletion {
                _status.value = ScanStatus.Stopped
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

            val device = JadeBleDevice.fromScan(
                peripheral = peripheral,
                isBonded = advertisement.isBonded()
            )

            addBluetoothDevice(device)
        }
    }

    protected fun addBluetoothDevice(newDevice: GreenDevice) {
        bleDevices.value.find { it.connectionIdentifier == newDevice.connectionIdentifier }?.also { oldDevice ->
                newDevice.peripheral?.also {
                    oldDevice.updateFromScan(it)
                }
            } ?: run {
            // Add it if new
            bleDevices.value += newDevice
        }
    }

    private fun pauseBluetoothScanning() = stopBluetoothScanning(CancellationException("pause"))

    private fun stopBluetoothScanning(cause: CancellationException? = null) {
        logger.i { "Stop BLE scanning" }
        scanScope.cancelChildren(cause = cause)
    }

    private fun addBleConnectedDevices() {
        sessionManager.getConnectedDevices().filter { it.isBle }.forEach {
            addBluetoothDevice(it)
        }
    }

    open fun refreshDevices(){
        logger.i { "Refresh device list" }
        cleanupBleDevices(cleanAll = true)
    }

    fun startDeviceDiscovery() {
        deviceDiscovery.compareAndSet(expect = false, update = true).ifTrue {
            logger.d { "startDeviceDiscovery" }
        }
    }

    fun stopDeviceDiscovery() {
        deviceDiscovery.compareAndSet(true, false).ifTrue {
            logger.d { "stopDeviceDiscovery" }
        }
    }

    companion object : Loggable() {
        const val JADE = "6e400001-b5a3-f393-e0a9-e50e24dcca9e"
        const val JADE_MTU = 517
    }
}