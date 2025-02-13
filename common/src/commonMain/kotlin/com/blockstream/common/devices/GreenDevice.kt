package com.blockstream.common.devices

import com.blockstream.common.extensions.tryCatchNull
import com.blockstream.common.gdk.Gdk
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.device.GdkHardwareWallet
import com.blockstream.common.gdk.device.HardwareConnectInteraction
import com.blockstream.green.utils.Loggable
import com.blockstream.jade.firmware.FirmwareUpdateState
import com.juul.kable.Peripheral
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.Clock
import kotlin.properties.Delegates

enum class DeviceState {
    CONNECTED, DISCONNECTED
}

interface DeviceOperatingNetwork {
    suspend fun getOperatingNetworkForEnviroment(greenDevice: GreenDevice, gdk: Gdk, isTestnet: Boolean): Network?
    suspend fun getOperatingNetwork(greenDevice: GreenDevice, gdk: Gdk, interaction: HardwareConnectInteraction): Network?
}

interface GreenDevice: DeviceOperatingNetwork {
    val connectionIdentifier: String
    val uniqueIdentifier: String
    val peripheral: Peripheral?
    var gdkHardwareWallet: GdkHardwareWallet?
    val deviceBrand: DeviceBrand
    val deviceModel: DeviceModel?
    val type: ConnectionType
    val isBonded: Boolean
    val isUsb: Boolean
    val isBle: Boolean
    val deviceState: StateFlow<DeviceState>
    val firmwareState: StateFlow<FirmwareUpdateState?>
    val name: String
    val manufacturer: String?
    val isJade: Boolean
    val isTrezor: Boolean
    val isLedger: Boolean
    val isOffline: Boolean
    val isConnected: Boolean
    val heartbeat: Long

    fun frozeHeartbeat()
    fun updateHeartbeat()

    fun disconnect()
    fun hasPermissions(): Boolean
    fun updateFromScan(newPeripheral: Peripheral)
    fun updateFirmwareState(status : FirmwareUpdateState)
    fun needsUsbPermissionsToIdentify(): Boolean
    fun askForUsbPermission(onSuccess: (() -> Unit), onError: ((throwable: Throwable?) -> Unit)? = null)
    fun canVerifyAddressOnDevice(account: Account): Boolean
}

fun GreenDevice.jadeDevice(): JadeDevice? = this as? JadeDevice

abstract class GreenDeviceImpl constructor(
    override val deviceBrand: DeviceBrand,
    override val type: ConnectionType,
    override var peripheral: Peripheral? = null,
    override val isBonded: Boolean
) : GreenDevice, DeviceOperatingNetwork {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _deviceState: MutableStateFlow<DeviceState> = MutableStateFlow(DeviceState.CONNECTED)
    override val deviceState: StateFlow<DeviceState> = _deviceState

    private val _firmwareState: MutableStateFlow<FirmwareUpdateState?> = MutableStateFlow(null)
    override val firmwareState: StateFlow<FirmwareUpdateState?> = _firmwareState

    override val name
        get() = peripheral?.name ?: deviceBrand.name

    // On Jade devices is not safe to use mac address as an id cause of RPA. Prefer using the unique name as a way to identify the device.
    override val connectionIdentifier: String
        get() = peripheral?.name ?: peripheral?.identifier?.toString() ?: hashCode().toString(10)

    override val uniqueIdentifier: String
        get() = name

    override val deviceModel: DeviceModel?
        get() = gdkHardwareWallet?.model

    final override var heartbeat: Long = Clock.System.now().toEpochMilliseconds()
        private set

    // Jade v1 has the controller manufacturer as a productName
    override val manufacturer
        get() = if (deviceBrand.isJade) deviceBrand.name else peripheral?.name

    override val isUsb
        get() = type == ConnectionType.USB

    override val isBle
        get() = type == ConnectionType.BLUETOOTH

    override val isOffline: Boolean
        get() = deviceState.value == DeviceState.DISCONNECTED

    override val isConnected: Boolean
        get() = deviceState.value == DeviceState.CONNECTED

    override val isJade: Boolean
        get() = deviceBrand.isJade

    override val isTrezor: Boolean
        get() = deviceBrand.isTrezor

    override val isLedger: Boolean
        get() = deviceBrand.isLedger

    override var gdkHardwareWallet: GdkHardwareWallet? by Delegates.observable(null) { _, _, gdkHardwareWallet ->
        logger.i { "Set GdkHardwareWallet" }

        gdkHardwareWallet?.disconnectEvent?.let {
            logger.i { "Subscribe to BLE disconnect event" }
            it.onEach { isDisconnected ->
                if (isDisconnected) {
                    logger.i { "BLE Disconnect event from device, marking it as offline" }
                    offline()
                }
            }.launchIn(scope = scope)
        }
    }

    override fun frozeHeartbeat() {
        logger.d { "frozeHeartbeat" }
        heartbeat = 0
    }

    override fun updateHeartbeat() {
        heartbeat = Clock.System.now().toEpochMilliseconds()
    }

    override fun askForUsbPermission(onSuccess: (() -> Unit), onError: ((throwable: Throwable?) -> Unit)?) { }

    override fun needsUsbPermissionsToIdentify(): Boolean {
        return isUsb && !hasPermissions()
    }

    override fun canVerifyAddressOnDevice(account: Account): Boolean {
        return !account.isLightning && (
                isJade ||
                        (isLedger && account.network.isLiquid && !account.network.isSinglesig) ||
                        (isLedger && !account.network.isLiquid && account.network.isSinglesig) ||
                        (isTrezor && !account.network.isLiquid && account.network.isSinglesig)
                )
    }

    override fun updateFromScan(newPeripheral: Peripheral) {
        // Update bleDevice as the it can be changed due to RPA
        peripheral = newPeripheral

        // Update timeout
        if (heartbeat > 0) {
            // Update timeout
            updateHeartbeat()
        }

        // Mark it as online if required
        _deviceState.compareAndSet(DeviceState.DISCONNECTED, DeviceState.CONNECTED)
    }

    fun offline() {
        logger.i { "Device went offline" }
        _deviceState.compareAndSet(DeviceState.CONNECTED, DeviceState.DISCONNECTED)
    }

    override fun updateFirmwareState(status : FirmwareUpdateState) {
        _firmwareState.value = status
    }

    override open fun hasPermissions(): Boolean = true

    override fun disconnect() {
        scope.cancel()

        tryCatchNull {
            gdkHardwareWallet?.disconnect()
        }

        gdkHardwareWallet = null

        if (isBle) {
            offline()
        }
    }

    companion object : Loggable()
}

