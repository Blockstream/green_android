@file:OptIn(ExperimentalUuidApi::class)

package com.blockstream.data.devices

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import com.blockstream.data.di.ApplicationScope
import com.blockstream.data.extensions.isBonded
import com.blockstream.data.extensions.isJade
import com.blockstream.data.managers.BluetoothManager
import com.blockstream.data.managers.DeviceManager
import com.blockstream.data.managers.SessionManager
import com.blockstream.utils.Loggable
import com.juul.kable.Peripheral
import com.juul.kable.PlatformAdvertisement
import java.lang.ref.WeakReference
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class DeviceManagerAndroid constructor(
    scope: ApplicationScope,
    val context: Context,
    sessionManager: SessionManager,
    bluetoothManager: BluetoothManager,
    val usbManager: UsbManager,
    supportedBleDevices: List<Uuid>,
    val deviceMapper: (
        deviceManager: DeviceManagerAndroid, usbDevice: UsbDevice?, bleService: Uuid?,
        peripheral: Peripheral?,
        isBonded: Boolean?
    ) -> AndroidDevice?
) : DeviceManager(scope, sessionManager, bluetoothManager, supportedBleDevices) {

    private var onPermissionSuccess: WeakReference<(() -> Unit)>? = null
    private var onPermissionError: WeakReference<((throwable: Throwable?) -> Unit)>? = null

    private var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {

            logger.i { "onReceive: ${intent.action} ${intent.extras}" }

            if (UsbManager.ACTION_USB_DEVICE_ATTACHED == intent.action) {
                scanUsbDevices()
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED == intent.action) {
                scanUsbDevices()
            } else if (ACTION_USB_PERMISSION == intent.action) {

                val device: UsbDevice? = IntentCompat.getParcelableExtra(intent, UsbManager.EXTRA_DEVICE, UsbDevice::class.java)

                logger.d { "Device $device" }
                logger.d { "Device has permission ${hasPermissions(device!!)}" }


                if (device != null && (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false) || hasPermissions(device))) {
                    device.apply {
                        logger.i { "Permission granted for device $device" }
                        onPermissionSuccess?.get()?.invoke()
                    }
                } else {
                    logger.i { "Permission denied for device $device" }
                    onPermissionError?.get()?.invoke(null)
                }
            }
        }
    }

    init {
        val intentFilter = IntentFilter().also {
            it.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            it.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            it.addAction(ACTION_USB_PERMISSION)
            it.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }

        ContextCompat.registerReceiver(
            context,
            broadcastReceiver,
            intentFilter,
            ContextCompat.RECEIVER_EXPORTED
        )

        scanUsbDevices()

    }

    override fun advertisedDevice(advertisement: PlatformAdvertisement) {
        val isJade = advertisement.isJade

        // Jade is added in Common code
        if (isJade) {
            super.advertisedDevice(advertisement)
        } else {
            val peripheral = Peripheral(advertisement)
            val bleService = advertisement.uuids.firstOrNull()

            deviceMapper.invoke(this, null, bleService, peripheral, advertisement.isBonded())
                ?.also {
                    addBluetoothDevice(it)
                }
        }
    }

    fun hasPermissions(device: UsbDevice) = usbManager.hasPermission(device)

    fun askForUsbPermissions(device: UsbDevice, onSuccess: (() -> Unit), onError: ((throwable: Throwable?) -> Unit)? = null) {
        onPermissionSuccess = WeakReference(onSuccess)
        onPermissionError = onError?.let { WeakReference(it) }
        val permissionIntent = PendingIntent.getBroadcast(context, 748, Intent(ACTION_USB_PERMISSION).also {
            // Cause of FLAG_IMMUTABLE OS won't give us the Extra Device
            it.putExtra(UsbManager.EXTRA_DEVICE, device)
        }, FLAG_IMMUTABLE)
        usbManager.requestPermission(device, permissionIntent)
    }

    override fun refreshDevices() {
        super.refreshDevices()

        scanUsbDevices()
    }

    fun scanUsbDevices() {
        logger.i { "Scan for USB devices" }

        val newUsbDevices = usbManager.deviceList.values

        // Disconnect devices
        val oldDevices = usbDevices.value.filter {
            if (newUsbDevices.contains(it.toAndroidDevice()?.usbDevice)) {
                true
            } else {
                it.toAndroidDevice()?.offline()
                false
            }
        }

        val newDevices = mutableListOf<AndroidDevice>()
        for (usbDevice in newUsbDevices) {
            if (oldDevices.find { it.toAndroidDevice()?.usbDevice == usbDevice } == null) {

                // Jade or UsbDeviceMapper
                (JadeUsbDevice.fromUsbDevice(deviceManager = this, usbDevice = usbDevice)
                    ?: deviceMapper.invoke(this, usbDevice, null, null, null))?.let {
                    newDevices += it
                }
            }
        }

        usbDevices.value = oldDevices + newDevices
    }

    companion object : Loggable() {
        private const val ACTION_USB_PERMISSION = "com.blockstream.green.USB_PERMISSION"
    }
}