package com.blockstream.common.devices

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import com.benasher44.uuid.Uuid
import com.blockstream.common.di.ApplicationScope
import com.blockstream.common.extensions.isBonded
import com.blockstream.common.extensions.isJade
import com.blockstream.common.managers.BluetoothManager
import com.blockstream.common.managers.DeviceManager
import com.blockstream.common.managers.SessionManager
import com.blockstream.common.utils.Loggable
import com.juul.kable.Peripheral
import com.juul.kable.PlatformAdvertisement
import com.juul.kable.peripheral
import java.lang.ref.WeakReference

class DeviceManagerAndroid constructor(
    scope: ApplicationScope,
    val activityProvider: ActivityProvider, // provide Activity reference needed by NfcAdapter
    val context: Context,
    sessionManager: SessionManager,
    bluetoothManager: BluetoothManager,
    val usbManager: UsbManager,
    supportedBleDevices: List<String>,
    val deviceMapper: (
        deviceManager: DeviceManagerAndroid,
        usbDevice: UsbDevice?,
        bleService: Uuid?,
        peripheral: Peripheral?,
        isBonded: Boolean?,
        nfcDevice: NfcDevice?,
        activityProvider: ActivityProvider?,
    ) -> AndroidDevice?
): CardListener, DeviceManager(scope, sessionManager, bluetoothManager, supportedBleDevices) {

    private val nfcAdapter = NfcAdapter.getDefaultAdapter(context)

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
        logger.i { "SATODEBUG DeviceManagerAndroid init() start" }
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


        scanNfcDevices() // SATODEBUG
        scanUsbDevices()

        logger.i { "SATODEBUG DeviceManagerAndroid init() end" }
    }

    // SATODEBUG: for bluetooth?
    override fun advertisedDevice(advertisement: PlatformAdvertisement) {
        logger.i { "SATODEBUG advertisedDevice() start" }
        val isJade = advertisement.isJade

        // Jade is added in Common code
        if (isJade) {
            super.advertisedDevice(advertisement)
        } else {
            val peripheral = scope.peripheral(advertisement)
            val bleService = advertisement.uuids.firstOrNull()

            deviceMapper.invoke(this, null, bleService, peripheral, advertisement.isBonded(), null,null)
                ?.also {
                    addBluetoothDevice(it)
                }
        }
        logger.i { "SATODEBUG advertisedDevice() end" }
    }

    fun hasPermissions(device: UsbDevice) = usbManager.hasPermission(device)

    fun askForUsbPermissions(device: UsbDevice, onSuccess: (() -> Unit), onError: ((throwable: Throwable?) -> Unit)? = null) {
        logger.i { "SATODEBUG askForUsbPermissions() start" }
        onPermissionSuccess = WeakReference(onSuccess)
        onPermissionError = onError?.let { WeakReference(it) }
        val permissionIntent = PendingIntent.getBroadcast(context, 748, Intent(ACTION_USB_PERMISSION).also {
            // Cause of FLAG_IMMUTABLE OS won't give us the Extra Device
            it.putExtra(UsbManager.EXTRA_DEVICE, device)
        }, FLAG_IMMUTABLE)
        usbManager.requestPermission(device, permissionIntent)
        logger.i { "SATODEBUG askForUsbPermissions() end" }
    }

    override fun refreshDevices(){
        super.refreshDevices()
        logger.i { "SATODEBUG refreshDevices() start" }
        scanNfcDevices() // SATODEBUG
        scanUsbDevices()
        logger.i { "SATODEBUG refreshDevices() end" }
    }

    fun scanUsbDevices() {
        logger.i { "SATODEBUG scanUsbDevices() start" }
        logger.i { "Scan for USB devices" }

        val newUsbDevices = usbManager.deviceList.values

        logger.i { "SATODEBUG scanUsbDevices() newUsbDevices: $newUsbDevices" }

        // Disconnect devices
        val oldDevices = usbDevices.value.filter {
            logger.i { "SATODEBUG scanUsbDevices() usbDevice: $it" }
            if(newUsbDevices.contains(it.toAndroidDevice()?.usbDevice)){
                true
            }else{
                it.toAndroidDevice()?.offline()
                false
            }
        }
        logger.i { "SATODEBUG scanUsbDevices() oldDevices: $oldDevices" }

        val newDevices = mutableListOf<AndroidDevice>()
        for (usbDevice in newUsbDevices){
            logger.i { "SATODEBUG scanUsbDevices() usbDevice: $usbDevice" }
            if(oldDevices.find { it.toAndroidDevice()?.usbDevice == usbDevice } == null) {

                // Jade or UsbDeviceMapper
                (JadeUsbDevice.fromUsbDevice(deviceManager = this, usbDevice = usbDevice)
                    ?: deviceMapper.invoke(this, usbDevice, null, null, null, null,null))?.let {
                    newDevices += it
                }
            }
        }
        logger.i { "SATODEBUG scanUsbDevices() newDevices: $newDevices" }

        usbDevices.value = oldDevices + newDevices
        logger.i { "SATODEBUG scanUsbDevices() end" }
    }

    fun scanNfcDevices() {
        logger.i { "SATODEBUG scanNfcDevices() start" }
        logger.i { "Scan for NFC devices" }

        val cardManager = NfcCardManager()
        cardManager.setCardListener(this)
        cardManager.start()
        logger.i { "SATODEBUG scanNfcDevices() after cardManager start" }

        val activity = activityProvider.getCurrentActivity()

        nfcAdapter?.enableReaderMode(
            activity,
            cardManager,
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null
        )
        logger.d { "SATODEBUG scanNfcDevices() after nfcAdapter" }


        logger.i { "SATODEBUG scanNfcDevices() end" }
    }

    override fun onConnected(channel: CardChannel) {

        println("SATODEBUG DeviceManagerAndroid onConnected: Card is connected")
        for (nfcDeviceType in NfcDeviceType.entries) {
            try {
                val cmdSet = SatochipCommandSet(channel)

                // try to select applet according to device candidate
                cmdSet.cardSelect(nfcDeviceType).checkOK()

                // add device
                val nfcDevice = NfcDevice(NfcDeviceType.SATOCHIP)
                val newDevices = mutableListOf<AndroidDevice>()
                deviceMapper.invoke(this, null, null, null, null, nfcDevice, activityProvider)
                    ?.let {
                        newDevices += it
                    }
                println("SATODEBUG DeviceManagerAndroid readCard newDevices: ${newDevices}")
                nfcDevices.value = newDevices

                // disconnect card
                println("SATODEBUG DeviceManagerAndroid onConnected: trigger disconnection!")
                onDisconnected()

                // stop polling
                val activity = activityProvider.getCurrentActivity()
                nfcAdapter?.disableReaderMode(activity)

                // should probably avoid to connect to multiple NFC devices at the same time?
                return
            } catch (e: Exception) {
                println("SATODEBUG DeviceManagerAndroid onConnected: failed to connect to device: $nfcDeviceType")
            }
        }
    }

    override fun onDisconnected() {
        println("SATODEBUG DeviceManagerAndroid onDisconnected: Card disconnected!")
    }


    companion object : Loggable() {
        private const val ACTION_USB_PERMISSION = "com.blockstream.green.USB_PERMISSION"
    }
}