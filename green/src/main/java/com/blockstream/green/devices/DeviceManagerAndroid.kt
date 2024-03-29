package com.blockstream.green.devices

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
import android.os.ParcelUuid
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.lifecycle.MutableLiveData
import com.blockstream.common.di.ApplicationScope
import com.blockstream.common.managers.DeviceManager
import com.blockstream.common.managers.SessionManager
import com.blockstream.jade.JadeBleImpl
import com.btchip.comm.LedgerDeviceBLE
import com.polidea.rxandroidble3.RxBleClient
import com.polidea.rxandroidble3.scan.ScanCallbackType
import com.polidea.rxandroidble3.scan.ScanFilter
import com.polidea.rxandroidble3.scan.ScanSettings
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import mu.KLogging
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit


class DeviceManagerAndroid constructor(
    scope: ApplicationScope,
    private val context: Context,
    val sessionManager: SessionManager,
    val usbManager: UsbManager,
    private val rxBleClient: RxBleClient
): DeviceManager(scope) {
    private var onPermissionSuccess: WeakReference<(() -> Unit)>? = null
    private var onPermissionError: WeakReference<((throwable: Throwable?) -> Unit)>? = null

    var bleAdapterState = MutableLiveData(rxBleClient.state)

    private var pendingBleBonding: Device? = null
    private var bleScanDisposable = CompositeDisposable()

    private var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {

            logger.info { "onReceive: ${intent.action}" }

            if (UsbManager.ACTION_USB_DEVICE_ATTACHED == intent.action) {
                scanDevices()
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED == intent.action) {
                scanDevices()
            } else if (ACTION_USB_PERMISSION == intent.action) {

                val device: UsbDevice? = IntentCompat.getParcelableExtra(intent, UsbManager.EXTRA_DEVICE, UsbDevice::class.java)

                if (device != null && (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false) || hasPermissions(device))) {
                    device.apply {
                        logger.info { "Permission granted for device $device" }
                        onPermissionSuccess?.get()?.invoke()
                    }
                } else {
                    logger.info { "Permission denied for device $device" }
                    onPermissionError?.get()?.invoke(null)
                }
            }else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED == intent.action){
                val bondedDevice = IntentCompat.getParcelableExtra(intent, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                val prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, 0)

                pendingBleBonding?.let{ pendingBondDevice ->
                    if(pendingBondDevice.bleDevice?.bluetoothDevice?.address == bondedDevice?.address){

                        // Is this needed?
                        if (prevState == BluetoothDevice.BOND_BONDING || prevState == BluetoothDevice.BOND_BONDED) {
                            if(bondedDevice?.bondState == BluetoothDevice.BOND_BONDED){
                                onPermissionSuccess?.get()?.invoke()
                                // As this device is no longer advertising itself prevent removal of the device
                                // It can be removed by refreshDevices method
                                pendingBondDevice.timeout = 0
                            }else{
                                logger.error { "Bonding failed for ${pendingBondDevice.name} : ${pendingBondDevice.bleDevice?.bluetoothDevice?.address}" }
                                // Reset timeout
                                pendingBondDevice.timeout = SystemClock.elapsedRealtimeNanos()
                            }

                            onPermissionSuccess = null
                            onPermissionError = null
                        }
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

        ContextCompat.registerReceiver(
            context,
            broadcastReceiver,
            intentFilter,
            ContextCompat.RECEIVER_EXPORTED
        )

        scanDevices()
    }

    fun startBluetoothScanning() {
        if(bleScanDisposable.size() > 0){
            return
        }

        logger.info { "Start BLE scanning" }

        addBleConnectedDevices()

        Observable.interval( 0, 1, TimeUnit.SECONDS)
            .doOnNext { addBleConnectedDevices() }
            .map { SystemClock.elapsedRealtimeNanos() - 5000000000 } // 5 seconds
            .subscribe{ ts ->
                _bleDevices.value = _bleDevices.value.map { it.toAndroidDevice() }.filter {
                    !it.isOffline && (it.timeout == 0L || it.timeout > ts)
                }
            }.addTo(bleScanDisposable)

        rxBleClient
            .observeStateChanges()
            .startWithItem(rxBleClient.state)
            .distinctUntilChanged()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .switchMap { state: RxBleClient.State? ->

                bleAdapterState.value = state

                // Scan only when Bluetooth is Ready
                if(state == RxBleClient.State.READY ){
                    logger.info { "BLE is ready" }

                    val scanSettings = ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                        .build()

                    // Filter - filter supported hw
                    val scanFilter = SupportedBleUuid.map {
                        ScanFilter.Builder()
                            .setServiceUuid(it)
                            .build()
                    }.toTypedArray()

                    return@switchMap  rxBleClient.scanBleDevices(scanSettings, *scanFilter)
                        .onErrorComplete() // Important! Prevent errors from escaping

                }else{
                    logger.info { "BLE - is not ready" }

                    // Empty BLE devices
                    _bleDevices.value = listOf()
                    return@switchMap Observable.empty()
                }
            }
            .doOnError {
                it.printStackTrace()
            }
            .subscribeBy(
                onError = {
                    // Errors are not expected to happen
                    it.printStackTrace()
                },
                onNext = {
                    val device = Device.fromScan(
                        this,
                        it.bleDevice,
                        it.scanRecord.serviceUuids?.firstOrNull()
                    )

                    if (it.callbackType == ScanCallbackType.CALLBACK_TYPE_FIRST_MATCH || it.callbackType == ScanCallbackType.CALLBACK_TYPE_ALL_MATCHES) {

                        addBluetoothDevice(device)

                    } else if (it.callbackType == ScanCallbackType.CALLBACK_TYPE_MATCH_LOST) {
                        _bleDevices.value = _bleDevices.value.filter { dev ->
                            dev.connectionIdentifier != device.connectionIdentifier
                        }
                    }
                }
            ).addTo(bleScanDisposable)
    }

    private fun addBluetoothDevice(newDevice: Device){
        _bleDevices.value.find { it.connectionIdentifier == newDevice.connectionIdentifier }?.also { oldDevice ->
            newDevice.bleDevice?.let{
                oldDevice.toAndroidDevice().updateFromScan(it)
            }
        } ?: run {
            // Add it if new
            _bleDevices.value = _bleDevices.value + newDevice
        }
    }

    fun pauseBluetoothScanning() {
        if(bleScanDisposable.size() == 0){
            return
        }

        logger.info { "Pause BLE scanning" }
        bleScanDisposable.clear()
    }

    private fun addBleConnectedDevices(){
        sessionManager.getConnectedDevices().filter { it.isBle }.forEach {
            addBluetoothDevice(it as Device)
        }
    }

    fun hasPermissions(device: UsbDevice) = usbManager.hasPermission(device)

    fun askForPermissions(device: UsbDevice, onSuccess: (() -> Unit), onError: ((throwable: Throwable?) -> Unit)? = null) {
        onPermissionSuccess = WeakReference(onSuccess)
        onPermissionError = onError?.let { WeakReference(it) }
        val permissionIntent = PendingIntent.getBroadcast(context, 748, Intent(ACTION_USB_PERMISSION).also {
            // Cause of FLAG_IMMUTABLE OS won't give us the Extra Device
            it.putExtra(UsbManager.EXTRA_DEVICE, device)
        }, FLAG_IMMUTABLE)
        usbManager.requestPermission(device, permissionIntent)
    }

    fun refreshDevices(){
        logger.info { "Refresh device list" }

        _bleDevices.value = listOf()
        scanDevices()
    }

    fun scanDevices() {
        logger.info { "Scan for USB devices" }

        val newUsbDevices = usbManager.deviceList.values

        // Disconnect devices
        val oldDevices = _usbDevices.value.filter {
            if(newUsbDevices.contains(it.toAndroidDevice().usbDevice)){
                true
            }else{
                it.toAndroidDevice().offline()
                false
            }
        }

        val newDevices = mutableListOf<Device>()
        for (usbDevice in newUsbDevices){
            if(oldDevices.find { it.toAndroidDevice().usbDevice == usbDevice } == null){
                Device.fromDevice(this, usbDevice)?.let{
                    newDevices += it
                }
            }
        }

        _usbDevices.value = oldDevices + newDevices
    }

    @SuppressLint("MissingPermission")
    fun bondDevice(
        device: Device,
        onSuccess: (() -> Unit),
        onError: ((throwable: Throwable?) -> Unit)
    ){
        device.bleDevice?.let {

            onPermissionSuccess = WeakReference(onSuccess)
            onPermissionError = WeakReference(onError)
            pendingBleBonding = device

            if(it.bluetoothDevice.createBond()){
                // Extend timeout for 30 seconds until paired
                // BLE when connected is not advertised, so it will remain visible for 30"
                device.timeout += 30000000000 // 30 extra seconds to pair
            }else{
                onError.invoke(Throwable("id_please_reconnect_your_hardware"))
            }
        }
    }
    fun getAndroidDevice(deviceId: String?) = getDeviceOrNull(deviceId = deviceId)?.toAndroidDevice()

    companion object : KLogging() {
        private const val ACTION_USB_PERMISSION = "com.blockstream.green.USB_PERMISSION"

        // Supported BLE Devices
        private val SupportedBleUuid = listOf(ParcelUuid(LedgerDeviceBLE.SERVICE_UUID), ParcelUuid(JadeBleImpl.IO_SERVICE_UUID))
    }
}