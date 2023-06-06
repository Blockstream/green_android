package com.blockstream.green.devices

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.hardware.usb.UsbDevice
import android.os.ParcelUuid
import android.os.SystemClock
import com.blockstream.common.gdk.device.DeviceBrand
import com.blockstream.common.gdk.device.DeviceInterface
import com.blockstream.common.gdk.device.DeviceState
import com.blockstream.common.gdk.device.GdkHardwareWallet
import com.blockstream.green.BuildConfig
import com.btchip.comm.LedgerDeviceBLE
import com.polidea.rxandroidble3.RxBleDevice
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import mu.KLogging
import kotlin.properties.Delegates

/*
 * If a BLE device, DeviceManager will update bleDevice if required due to RPA, it's safe to consider
 * Device to be updated with latest broadcasted RxBleDevice
 */
class Device constructor(
    val type: ConnectionType,
    val deviceManager: DeviceManager,
    val usbDevice: UsbDevice? = null,
    var bleDevice: RxBleDevice? = null,
    val bleService: ParcelUuid? = null,
): DeviceInterface {

    private val _deviceState : MutableStateFlow<DeviceState> = MutableStateFlow(DeviceState.SCANNED)
    override val deviceState
        get() = _deviceState.asStateFlow()

    private val bleDisposables = CompositeDisposable()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override var gdkHardwareWallet: GdkHardwareWallet? by Delegates.observable(null) { _, _, gdkHardwareWallet ->
        logger.info { "Set GdkHardwareWallet" }

        gdkHardwareWallet?.disconnectEvent?.let {
            logger.info { "Subscribe to BLE disconnect event" }
            it.onEach { isDisconnected ->
                if(isDisconnected){
                    logger.info { "BLE Disconnect event from device, marking it as offline" }
                    offline()
                }
            }.launchIn(scope = scope)
        }
    }
    override val isJade: Boolean
        get() = deviceBrand.isJade
    override val isTrezor: Boolean
        get() = deviceBrand.isTrezor
    override val isLedger: Boolean
        get() = deviceBrand.isLedger

    val usbManager
        get() = deviceManager.usbManager

    override val isOffline = deviceState.value == DeviceState.DISCONNECTED

    val supportsLiquid by lazy {
        !deviceBrand.isTrezor
    }

    val canSwitchNetwork by lazy {
        deviceBrand.isJade || deviceBrand.isTrezor
    }

    fun askForPermissionOrBond(onSuccess: (() -> Unit), onError: ((throwable: Throwable?) -> Unit)? = null) {
        usbDevice?.let {
            deviceManager.askForPermissions(device = it, onError = onError, onSuccess = onSuccess)
        }

        bleDevice?.let {
            deviceManager.bondDevice(this, {
                // deviceState.postValue(DeviceState.CONNECTED)
                onSuccess.invoke()
            }, {
                onError?.invoke(it)
            })
        }
    }

    var timeout: Long = 0

    // On Jade devices is not safe to use mac address as an id cause of RPA. Prefer using the unique name as a way to identify the device.
    override val id: String by lazy {
        usbDevice?.deviceId?.toString(10) ?: (if(deviceBrand.isJade) name else bleDevice?.bluetoothDevice?.address) ?: hashCode().toString(10)
    }

    val uniqueIdentifier: String
        get() = try {
            (if(isBle) name else usbDevice?.serialNumber) ?: hashCode().toString(10)
        }catch (e: Exception){
            if(BuildConfig.DEBUG) {
                e.printStackTrace()
            }
            hashCode().toString(10)
        }

    override val name
        @SuppressLint("MissingPermission")
        get() = (if (deviceBrand.isJade && isUsb) "Jade" else usbDevice?.productName
            ?: bleDevice?.bluetoothDevice?.name) ?: deviceBrand.name

    // Jade v1 has the controller manufacturer as a productName
    val manufacturer
        @SuppressLint("MissingPermission")
        get() = if (deviceBrand.isJade) deviceBrand.name else usbDevice?.productName
            ?: bleDevice?.bluetoothDevice?.name

    val vendorId
        get() = usbDevice?.vendorId

    val productId
        get() = usbDevice?.productId

    override val isUsb
        get() = type == ConnectionType.USB

    override val isBle
        get() = type == ConnectionType.BLUETOOTH

    override val deviceBrand by lazy {
        when {
            (usbDevice?.vendorId == VENDOR_TREZOR || usbDevice?.vendorId == VENDOR_TREZOR_V2) -> DeviceBrand.Trezor
            (bleService == ParcelUuid(LedgerDeviceBLE.SERVICE_UUID) || usbDevice?.vendorId == VENDOR_BTCHIP || usbDevice?.vendorId == VENDOR_LEDGER) -> DeviceBrand.Ledger
            else -> DeviceBrand.Blockstream
        }
    }

    fun hasPermissions(): Boolean {
        return usbDevice?.let { deviceManager.hasPermissions(it) } ?: false
    }

    @SuppressLint("MissingPermission")
    fun isBonded() = bleDevice?.bluetoothDevice?.bondState == BluetoothDevice.BOND_BONDED

    fun hasPermissionsOrIsBonded(): Boolean {
        return if (isUsb) {
            hasPermissions()
        } else {
            isBonded()
        }
    }

    fun needsUsbPermissionsToIdentify(): Boolean {
        return isUsb && !hasPermissions()
    }

    fun handleBondingByHwwImplementation(): Boolean {
        return isBle && deviceBrand.isJade
    }

    fun offline() {
        logger.info { "Device went offline" }
        _deviceState.compareAndSet(DeviceState.SCANNED, DeviceState.DISCONNECTED)
    }

    override fun disconnect() {
        bleDisposables.clear()
        scope.cancel()
        gdkHardwareWallet?.disconnect()
        gdkHardwareWallet = null

        if(isBle) {
            offline()
        }
    }

    fun updateFromScan(newBleDevice: RxBleDevice) {
        // Update bleDevice as the it can be changed due to RPA
        bleDevice = newBleDevice

        // Update timeout
        timeout = SystemClock.elapsedRealtimeNanos()

        // Mark it as online if required
        _deviceState.compareAndSet(DeviceState.DISCONNECTED, DeviceState.SCANNED)
    }

    enum class ConnectionType {
        USB, BLUETOOTH
    }

    companion object : KLogging() {
        const val VENDOR_BTCHIP = 0x2581
        const val VENDOR_LEDGER = 0x2c97
        const val VENDOR_TREZOR = 0x534c
        const val VENDOR_TREZOR_V2 = 0x1209
        const val VENDOR_JADE_A = 0x10c4
        const val VENDOR_JADE_B = 0x1a86

        private fun hasSuportedVendorId(usbDevice: UsbDevice) : Boolean{
            val vId = usbDevice.vendorId
            return (vId == VENDOR_BTCHIP ||
                    vId == VENDOR_LEDGER ||
                    vId == VENDOR_TREZOR ||
                    vId == VENDOR_TREZOR_V2 ||
                    vId == VENDOR_JADE_A ||
                    vId == VENDOR_JADE_B)
        }

        fun fromDevice(deviceManager: DeviceManager, usbDevice: UsbDevice): Device? {
            if(hasSuportedVendorId(usbDevice)){
                return Device(
                    ConnectionType.USB,
                    deviceManager,
                    usbDevice = usbDevice
                )
            }
            return null
        }

        fun fromScan(
            deviceManager: DeviceManager,
            bleDevice: RxBleDevice,
            bleService: ParcelUuid?
        ): Device {
            return Device(
                ConnectionType.BLUETOOTH,
                deviceManager,
                bleDevice = bleDevice,
                bleService = bleService
            ).also {
                it.timeout = SystemClock.elapsedRealtimeNanos()
            }
        }
    }
}