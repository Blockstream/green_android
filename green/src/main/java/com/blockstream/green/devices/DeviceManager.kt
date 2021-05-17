package com.blockstream.green.devices

import android.app.PendingIntent
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.ParcelUuid
import android.os.SystemClock
import androidx.lifecycle.MutableLiveData
import com.blockstream.green.gdk.async
import com.btchip.comm.LedgerDeviceBLE
import com.greenaddress.jade.JadeBleImpl
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.scan.ScanCallbackType
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanSettings
import hu.akarnokd.rxjava3.bridge.RxJavaBridge
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.subjects.BehaviorSubject
import mu.KLogging
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.TimeUnit


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

    var bleAdapterState = MutableLiveData(rxBleClient.state)

    private var pendingBleBonding: Device? = null
    private var bleScanDisposable = CompositeDisposable()

    private var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
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
                            logger.info { "Permission granted for device $device" }
                            onPermissionSuccess?.get()?.invoke()
                        }
                    } else {
                        logger.info { "Permission denied for device $device" }
                    }
                }
            }else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED == intent.action){
                val bondedDevice  = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val prevState  = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, 0)

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

        context.registerReceiver(broadcastReceiver, intentFilter)

        scanDevices()
    }

    fun startBluetoothScanning() {
        logger.info { "Start BLE scanning" }

        if(bleScanDisposable.size() > 0){
            logger.info { "Skip starting bluetooth scanning as it's already started" }
        }

        // Is this actually works?
        addBleConnectedDevices()

        Observable.interval( 2, 1, TimeUnit.SECONDS)
            .doOnNext { addBleConnectedDevices() }
            .map { SystemClock.elapsedRealtimeNanos() - 5000000000 } // 5 seconds
            .subscribe{ ts ->
                bluetoothDevicesSubject.onNext(bluetoothDevicesSubject.value.filter {
                    it.timeout == 0L || it.timeout > ts
                })

            }.addTo(bleScanDisposable)

        rxBleClient
            .observeStateChanges()
            .`as`(RxJavaBridge.toV3Observable())
            .startWithItem(rxBleClient.state)
            .distinctUntilChanged()
            .async()
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
                        .`as`(RxJavaBridge.toV3Observable())
                        .onErrorComplete() // Important! Prevent errors from escaping

                }else{
                    logger.info { "BLE - is not ready" }

                    // Empty BLE devices
                    bluetoothDevicesSubject.onNext(listOf())
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
                        bluetoothDevicesSubject.onNext(bluetoothDevicesSubject.value.filter { dev ->
                            dev.id != device.id
                        })
                    }
                }
            ).addTo(bleScanDisposable)
    }

    private fun addBluetoothDevice(device: Device){
        bluetoothDevicesSubject.value.find { it.id == device.id }?.also { device ->
            // Update timeout
            device.timeout = SystemClock.elapsedRealtimeNanos()
        } ?: run {
            // Add it if new
            bluetoothDevicesSubject.onNext(bluetoothDevicesSubject.value + device)
        }
    }

    fun pauseBluetoothScanning() {
        logger.info { "Pause BLE scanning" }
        bleScanDisposable.clear()
    }

    // TODO Tests needed to check if this method actually works
    private fun addBleConnectedDevices(){
        try{
            if(bluetoothManager.adapter == null){
                // Adapter not available
                return
            }

            val connectedDevices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)

            // Add supported devices that are already connected (and may not be 'advertising')
            addSupportedDevices(connectedDevices)
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun addSupportedDevices(devices: List<BluetoothDevice>) {
        for(connectedDevice in devices){
            val bleDevice = rxBleClient.getBleDevice(connectedDevice.address)

            bleDevice
                .establishConnection(false)
                .`as`(RxJavaBridge.toV3Observable())
                .singleOrError()
                .flatMap { it.discoverServices().`as`(RxJavaBridge.toV3Single()) }
                .async()
                .subscribeBy(
                    onError = {
                        logger.error { "Error trying to browse services for ${connectedDevice.name}" }
                        it.printStackTrace()
                    },
                    onSuccess = {
                        for(service in it.bluetoothGattServices){
                            val serviceId = service.uuid

                            if(SupportedBleUuid.contains(ParcelUuid(serviceId))){
                                val device = Device.fromScan(
                                    this,
                                    bleDevice,
                                    ParcelUuid(serviceId)
                                )

                                addBluetoothDevice(device)
                                return@subscribeBy
                            }
                        }
                    }
                )
        }
    }

    fun getDevices(): Observable<List<Device>> = devicesSubject.hide()

    @Deprecated("Handling intents it no longer used as we register a broadcast receiver")
    fun handleIntent(intent: Intent) {
        logger.info { "Intent action: ${intent.action}" }

        when (intent.action) {
            UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                    ?.let { usbDevice ->
                        // Handle if required
                    }
            }

            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                    ?.let {
                        // Handle if required
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

    fun refreshDevices(){
        logger.info { "Refresh device list" }

        bluetoothDevicesSubject.onNext(listOf())
        scanDevices()
    }

    fun scanDevices() {
        logger.info { "Scan for USB devices" }

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

    fun bondDevice(
        device: Device,
        onSuccess: (() -> Unit),
        onError: ((throwable: Throwable) -> Unit)
    ){
        device.bleDevice?.let {

            onPermissionSuccess = WeakReference(onSuccess)
            onPermissionError = WeakReference(onError)
            pendingBleBonding = device

            if(it.bluetoothDevice.createBond()){
                // Extend timeout for a minute until paired
                // BLE when connected is not advertised, so it will remain visible for 30"
                device.timeout += 30000000000 // 30 extra seconds to pair
            }else{
                onError.invoke(Throwable("id_please_reconnect_your_hardware"))
            }
        }
    }

    fun getDevice(deviceId: Int): Device? {
        return usbDevicesSubject.value.find { it.id == deviceId } ?: bluetoothDevicesSubject.value.find { it.id == deviceId }
    }

    companion object : KLogging() {
        private const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"

        // Supported BLE Devices
        private val SupportedBleUuid = listOf(ParcelUuid(LedgerDeviceBLE.SERVICE_UUID), ParcelUuid(JadeBleImpl.IO_SERVICE_UUID))
    }
}