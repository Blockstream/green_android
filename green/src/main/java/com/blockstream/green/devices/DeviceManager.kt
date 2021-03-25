package com.blockstream.green.devices

import android.app.PendingIntent
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleDevice
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import mu.KLogging
import java.lang.ref.WeakReference
import java.util.*


class DeviceManager constructor(
    private val context: Context,
    private val usbManager: UsbManager,
    private val bluetoothManager: BluetoothManager,
    private val rxBleClient: RxBleClient
) {
    private var onPermissionSuccess: WeakReference<(() -> Unit)>? = null
    private var onPermissionError: WeakReference<((throwable: Throwable) -> Unit)>? = null

    private val usbDevicesSubject = BehaviorSubject.createDefault<List<Device>>(listOf())
    private val bluetoothDevicesSubject = BehaviorSubject.createDefault<List<Device>>(listOf())

    private val devicesSubject = BehaviorSubject.combineLatest(
        usbDevicesSubject,
        bluetoothDevicesSubject
    ) { usb: List<Device>, bluetooth: List<Device> ->
        bluetooth + usb
    }

    var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            logger.info { "onReceive: ${intent.action}" }

            if (UsbManager.ACTION_USB_DEVICE_ATTACHED == intent.action) {
                scanDevices()
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED == intent.action) {
                scanDevices()
            } else if (ACTION_USB_PERMISSION == intent.action) {
                synchronized(this) {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.apply {
                            //call method to set up device communication
                            logger.info { "Permission granted for device $device" }
                            onPermissionSuccess?.get()?.invoke()
                        }
                    } else {
                        logger.info { "Permission denied for device $device" }
                    }
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

        context.registerReceiver(broadcastReceiver, intentFilter)

        scanDevices()
    }

    fun getDevices(): Observable<List<Device>> = devicesSubject.hide()

    fun handleIntent(intent: Intent) {
        logger.info { "action: ${intent.action}" }

        when (intent.action) {
            UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                    ?.let { usbDevice ->
                        // TODO handle if required
                    }
            }

            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                    ?.let {
                        // TODO handle if required
                    }
            }
        }
    }

    fun hasPermissions(device: UsbDevice) = usbManager.hasPermission(device)

    fun askForPermissions(device: UsbDevice, onSuccess: (() -> Unit)) {
        onPermissionSuccess = WeakReference(onSuccess)
        val permissionIntent =
            PendingIntent.getBroadcast(context, 0, Intent(ACTION_USB_PERMISSION), 0)
        usbManager.requestPermission(device, permissionIntent)
    }

    fun scanDevices() {
        logger.info { "scanDevices" }

        val newUsbDevices = usbManager.deviceList.values

        // Disconnect devices
        val oldDevices = usbDevicesSubject.value.filter {
            if(newUsbDevices.contains(it.usbDevice)){
                true
            }else{
                it.offline() // IS THiS NEEDED?
                it.deviceState.postValue(Device.DeviceState.DISCONNECTED)
                false
            }
        }

        val newDevices = mutableListOf<Device>()
        for (usbDevice in newUsbDevices){
            if(oldDevices.find { it.usbDevice == usbDevice } == null){
                newDevices += Device.fromDevice(this, usbDevice)
            }
        }

        usbDevicesSubject.onNext(oldDevices + newDevices)
    }

    fun getDevice(deviceId: Int): Device? {
        return usbDevicesSubject.value.find { it.id == deviceId } ?: bluetoothDevicesSubject.value.find { it.id == deviceId }
    }

    companion object : KLogging() {
        private const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
    }
}