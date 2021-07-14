package com.blockstream.green.devices

import android.bluetooth.BluetoothDevice
import android.hardware.usb.UsbDevice
import android.os.ParcelUuid
import android.os.SystemClock
import androidx.lifecycle.MutableLiveData
import com.blockstream.DeviceBrand
import com.btchip.comm.LedgerDeviceBLE
import com.greenaddress.greenapi.HWWallet
import com.greenaddress.jade.JadeBleImpl
import com.polidea.rxandroidble2.RxBleDevice
import kotlinx.parcelize.IgnoredOnParcel
import mu.KLogging


class Device constructor(
    val type: ConnectionType,
    val deviceManager: DeviceManager,
    val usbDevice: UsbDevice? = null,
    var bleDevice: RxBleDevice? = null,
    var bleService: ParcelUuid? = null,
) {
    enum class DeviceState {
        UNAUTHORIZED, CONNECTED, DISCONNECTED
    }

    val deviceState = MutableLiveData(DeviceState.UNAUTHORIZED)

    var hwWallet: HWWallet? = null

    val usbManager
        get() = deviceManager.usbManager

    init {
        if(hasPermissionsOrIsBonded()){
            deviceState.value = DeviceState.CONNECTED
        }else{
            deviceState.value = DeviceState.UNAUTHORIZED
        }
    }

    fun askForPermissionOrBond(onSuccess: (() -> Unit), onError: ((throwable: Throwable) -> Unit)) {
        usbDevice?.let {
            deviceManager.askForPermissions(it) {
                deviceState.postValue(DeviceState.CONNECTED)
                onSuccess.invoke()
            }
        }

        bleDevice?.let {
            deviceManager.bondDevice(this, {
                deviceState.postValue(DeviceState.CONNECTED)
                onSuccess.invoke()
            }, {
                onError.invoke(it)
            })
        }
    }

    var timeout: Long = 0

    open val id: Int by lazy {
        usbDevice?.deviceId ?: bleDevice?.bluetoothDevice?.address?.hashCode() ?: hashCode()
    }

    open val name
        get() = if (isJade && isUsb) "Jade" else usbDevice?.productName
            ?: bleDevice?.bluetoothDevice?.name

    // Jade v1 has the controller manufacturer as a productName
    open val manufacturer
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

    @IgnoredOnParcel
    val deviceBrand by lazy {
        when {
            isTrezor -> DeviceBrand.Trezor
            isLedger -> DeviceBrand.Ledger
            else -> DeviceBrand.Blockstream
        }
    }

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
        bleService == ParcelUuid(LedgerDeviceBLE.SERVICE_UUID) || usbDevice?.vendorId == VENDOR_BTCHIP || usbDevice?.vendorId == VENDOR_LEDGER
    }

    @IgnoredOnParcel
    val supportsLiquid by lazy {
        !isTrezor
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

        private fun hasSuportedVendorId(usbDevice: UsbDevice) : Boolean{
            val vId = usbDevice.vendorId
            return (vId == VENDOR_BTCHIP ||
                    vId == VENDOR_LEDGER ||
                    vId == VENDOR_TREZOR ||
                    vId == VENDOR_TREZOR_V2 ||
                    vId == VENDOR_JADE)
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