package com.blockstream.green.devices

import android.bluetooth.BluetoothDevice
import android.hardware.usb.UsbDevice
import android.os.ParcelUuid
import android.os.SystemClock
import androidx.lifecycle.MutableLiveData
import com.greenaddress.jade.JadeBleImpl
import com.polidea.rxandroidble2.RxBleDevice
import kotlinx.parcelize.IgnoredOnParcel
import mu.KLogging

open class Device constructor(
    val type: ConnectionType,
    val deviceManager: DeviceManager,
    val usbDevice: UsbDevice? = null,
    var bleDevice: RxBleDevice? = null,
    var bleService: ParcelUuid? = null,
) {
    enum class DeviceState {
        UNAUTHORIZED, CONNECTED, DISCONNECTED
    }

    val deviceState = MutableLiveData(DeviceState.DISCONNECTED)

    init {
        usbDevice?.let{
            if(deviceManager.hasPermissions(it)){
                deviceState.value = DeviceState.CONNECTED
            }else{
                deviceState.value = DeviceState.UNAUTHORIZED
            }
        }
    }

    fun askForPermissions(onSuccess: (() -> Unit)? = null){
        usbDevice?.let {
            deviceManager.askForPermissions(it) {
                deviceState.postValue(DeviceState.CONNECTED)
                onSuccess?.invoke()
            }
        }
    }

    var timeout: Long = 0

    open val id: Int by lazy {
        usbDevice?.deviceId ?: bleDevice?.bluetoothDevice?.address?.hashCode() ?: hashCode()
    }

    open val name
        get() = if (isJade && isUsb) "Jade" else usbDevice?.productName ?: bleDevice?.bluetoothDevice?.name

    open val manufacturer
        get() = if (isJade) "Blockstream" else usbDevice?.productName
            ?: "bleDevice?.bluetoothDevice?.name"

    val vendorId
        get() = usbDevice?.vendorId

    val productId
        get() = usbDevice?.productId

    val isUsb
        get() = type == ConnectionType.USB

    val isBluethooth
        get() = type == ConnectionType.BLUETOOTH


    @IgnoredOnParcel
    open val isJade by lazy {
        bleService == ParcelUuid(JadeBleImpl.IO_SERVICE_UUID) || usbDevice?.vendorId == VENDOR_JADE
    }

    @IgnoredOnParcel
    open val isTrezor by lazy {
        usbDevice?.vendorId == VENDOR_TREZOR || usbDevice?.vendorId == VENDOR_TREZOR_V2
    }

    @IgnoredOnParcel
    val isLedger by lazy {
        usbDevice?.vendorId == VENDOR_BTCHIP || usbDevice?.vendorId == VENDOR_LEDGER
    }

    fun hasPermissions(): Boolean {
        return usbDevice?.let { deviceManager.hasPermissions(it) } ?: false
    }

    fun isBonded(): Boolean {
        return bleDevice?.let { it.bluetoothDevice.bondState == BluetoothDevice.BOND_BONDED } ?: false
    }

    fun hasPermissionsOrIsBonded(): Boolean {
        return if(isUsb){
            hasPermissions()
        }else{
            isBonded()
        }
    }

    fun offline() {
        logger.info { "Device went offline" }
        deviceState.postValue(DeviceState.DISCONNECTED)
    }

    enum class ConnectionType {
        USB, BLUETOOTH
    }

    companion object : KLogging() {
        const val VENDOR_BTCHIP = 0x2581
        const val VENDOR_LEDGER = 0x2c97
        const val VENDOR_TREZOR = 0x534c
        const val VENDOR_TREZOR_V2 = 0x1209
        const val VENDOR_JADE = 0x10c4

        fun fromDevice(deviceManager: DeviceManager, usbDevice: UsbDevice): Device {
            return Device(
                ConnectionType.USB,
                deviceManager,
                usbDevice = usbDevice
            )
        }

        fun fromScan(deviceManager: DeviceManager, bleDevice: RxBleDevice, bleService: ParcelUuid?): Device {
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