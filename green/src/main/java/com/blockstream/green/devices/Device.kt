package com.blockstream.green.devices

import android.bluetooth.BluetoothDevice
import android.hardware.usb.UsbDevice
import android.os.ParcelUuid
import android.os.SystemClock
import com.blockstream.DeviceBrand
import com.btchip.comm.LedgerDeviceBLE
import com.greenaddress.greenapi.HWWallet
import com.greenaddress.jade.JadeBleImpl
import com.polidea.rxandroidble2.RxBleDevice
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import mu.KLogging
import java.lang.Exception
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
) {
    enum class DeviceState {
        SCANNED, DISCONNECTED
    }

    private val _deviceState : MutableStateFlow<DeviceState> = MutableStateFlow(DeviceState.SCANNED)
    val deviceState
        get() = _deviceState.asStateFlow()

    private val bleDisposables = CompositeDisposable()

    var hwWallet: HWWallet? by Delegates.observable(null) { _, _, hwWallet ->
        logger.info { "Set HWWallet" }

        bleDisposables.clear()

        hwWallet?.bleDisconnectEvent?.let {
            logger.info { "Subscribe to BLE disconnect event" }
            it.observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onError = { e ->
                    e.printStackTrace()
                },
                onNext = {
                    logger.info { "BLE Disconnect event from device, marking it as offline" }
                    offline()
                }).addTo(bleDisposables)
        }
    }

    val usbManager
        get() = deviceManager.usbManager

    val isOffline = deviceState.value == DeviceState.DISCONNECTED

    val isJade by lazy {
        bleService == ParcelUuid(JadeBleImpl.IO_SERVICE_UUID) || usbDevice?.vendorId == VENDOR_JADE_A  || usbDevice?.vendorId == VENDOR_JADE_B
    }

    val isTrezor by lazy {
        usbDevice?.vendorId == VENDOR_TREZOR || usbDevice?.vendorId == VENDOR_TREZOR_V2
    }

    val isLedger by lazy {
        bleService == ParcelUuid(LedgerDeviceBLE.SERVICE_UUID) || usbDevice?.vendorId == VENDOR_BTCHIP || usbDevice?.vendorId == VENDOR_LEDGER
    }

    val supportsLiquid by lazy {
        !isTrezor
    }

    val canSwitchNetwork by lazy {
        isJade || isTrezor
    }

    fun askForPermissionOrBond(onSuccess: (() -> Unit), onError: ((throwable: Throwable) -> Unit)) {
        usbDevice?.let {
            deviceManager.askForPermissions(it) {
                // deviceState.postValue(DeviceState.CONNECTED)
                onSuccess.invoke()
            }
        }

        bleDevice?.let {
            deviceManager.bondDevice(this, {
                // deviceState.postValue(DeviceState.CONNECTED)
                onSuccess.invoke()
            }, {
                onError.invoke(it)
            })
        }
    }

    var timeout: Long = 0

    // On Jade devices is not safe to use mac address as an id cause of RPA. Prefer using the unique name as a way to identify the device.
    val id: String by lazy {
        usbDevice?.deviceId?.toString(10) ?: (if(isJade) name else bleDevice?.bluetoothDevice?.address) ?: hashCode().toString(10)
    }

    val uniqueIdentifier: String
        get() = try {
            (if(isBle) name else usbDevice?.serialNumber) ?: hashCode().toString(10)
        }catch (e: Exception){
            e.printStackTrace()
            hashCode().toString(10)
        }

    val name
        get() = (if (isJade && isUsb) "Jade" else usbDevice?.productName
            ?: bleDevice?.bluetoothDevice?.name) ?: deviceBrand.name

    // Jade v1 has the controller manufacturer as a productName
    val manufacturer
        get() = if (isJade) deviceBrand.name else usbDevice?.productName
            ?: bleDevice?.bluetoothDevice?.name

    val vendorId
        get() = usbDevice?.vendorId

    val productId
        get() = usbDevice?.productId

    val isUsb
        get() = type == ConnectionType.USB

    val isBle
        get() = type == ConnectionType.BLUETOOTH

    val deviceBrand by lazy {
        when {
            isTrezor -> DeviceBrand.Trezor
            isLedger -> DeviceBrand.Ledger
            else -> DeviceBrand.Blockstream
        }
    }

    fun hasPermissions(): Boolean {
        return usbDevice?.let { deviceManager.hasPermissions(it) } ?: false
    }

    fun isBonded() = bleDevice?.bluetoothDevice?.bondState == BluetoothDevice.BOND_BONDED

    fun hasPermissionsOrIsBonded(): Boolean {
        return if (isUsb) {
            hasPermissions()
        } else {
            isBonded()
        }
    }

    fun handleBondingByHwwImplementation(): Boolean {
        return isBle && isJade
    }

    fun offline() {
        logger.info { "Device went offline" }
        _deviceState.compareAndSet(DeviceState.SCANNED, DeviceState.DISCONNECTED)
    }

    fun disconnect() {
        bleDisposables.clear()
        hwWallet?.disconnect()
        hwWallet = null

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