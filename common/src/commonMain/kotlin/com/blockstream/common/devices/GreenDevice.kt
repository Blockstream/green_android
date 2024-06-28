package com.blockstream.common.devices

import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.device.GdkHardwareWallet
import com.blockstream.common.utils.Loggable
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
    SCANNED, DISCONNECTED
}

open class GreenDevice constructor(
    val deviceBrand: DeviceBrand,
    val type: ConnectionType,
    var peripheral: Peripheral? = null,
    val isBonded: Boolean
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _deviceState: MutableStateFlow<DeviceState> = MutableStateFlow(DeviceState.SCANNED)
    val deviceState: StateFlow<DeviceState> = _deviceState

    open val name
        get() = peripheral?.name ?: deviceBrand.name

    // On Jade devices is not safe to use mac address as an id cause of RPA. Prefer using the unique name as a way to identify the device.
    open val connectionIdentifier: String
        get() = peripheral?.name ?: peripheral?.identifier?.toString() ?: hashCode().toString(10)

    open val uniqueIdentifier: String
        get() = name

    var timeout: Long = 0

    // Jade v1 has the controller manufacturer as a productName
    open val manufacturer
        get() = if (deviceBrand.isJade) deviceBrand.name else peripheral?.name

    val isUsb
        get() = type == ConnectionType.USB

    val isBle
        get() = type == ConnectionType.BLUETOOTH

    open val isOffline: Boolean
        get() = deviceState.value == DeviceState.DISCONNECTED

    val isJade: Boolean
        get() = deviceBrand.isJade
    val isTrezor: Boolean
        get() = deviceBrand.isTrezor
    val isLedger: Boolean
        get() = deviceBrand.isLedger

    var gdkHardwareWallet: GdkHardwareWallet? by Delegates.observable(null) { _, _, gdkHardwareWallet ->
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

    fun canVerifyAddressOnDevice(account: Account): Boolean {
        return !account.isLightning && (
                isJade ||
                        (isLedger && account.network.isLiquid && !account.network.isSinglesig) ||
                        (isLedger && !account.network.isLiquid && account.network.isSinglesig) ||
                        (isTrezor && !account.network.isLiquid && account.network.isSinglesig)
                )
    }

    fun updateFromScan(newPeripheral: Peripheral) {
        // Update bleDevice as the it can be changed due to RPA
        peripheral = newPeripheral

        // Update timeout
        timeout = Clock.System.now().toEpochMilliseconds()

        // Mark it as online if required
        _deviceState.compareAndSet(DeviceState.DISCONNECTED, DeviceState.SCANNED)
    }

    fun offline() {
        logger.i { "Device went offline" }
        _deviceState.compareAndSet(DeviceState.SCANNED, DeviceState.DISCONNECTED)
    }

    open fun hasPermissions(): Boolean = true

    open fun disconnect() {
        scope.cancel()

        gdkHardwareWallet?.disconnect()
        gdkHardwareWallet = null

        if (isBle) {
            offline()
        }
    }

    companion object : Loggable() {

        fun jadeFromScan(
            peripheral: Peripheral? = null,
            isBonded: Boolean = false
        ): GreenDevice {
            return GreenDevice(
                deviceBrand = DeviceBrand.Blockstream,
                type = ConnectionType.BLUETOOTH,
                peripheral = peripheral,
                isBonded = isBonded,
            ).also {
                it.timeout = Clock.System.now().toEpochMilliseconds()
            }
        }
    }
}