package com.blockstream.common.devices

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
import android.nfc.NfcAdapter
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


        scanNfcDevices()
        scanUsbDevices()
    }

    override fun advertisedDevice(advertisement: PlatformAdvertisement) {
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

    override fun refreshDevices(){
        super.refreshDevices()
        scanNfcDevices()
        scanUsbDevices()
    }

    fun scanUsbDevices() {
        logger.i { "Scan for USB devices" }

        val newUsbDevices = usbManager.deviceList.values

        // Disconnect devices
        val oldDevices = usbDevices.value.filter {
            if(newUsbDevices.contains(it.toAndroidDevice()?.usbDevice)){
                true
            }else{
                it.toAndroidDevice()?.offline()
                false
            }
        }

        val newDevices = mutableListOf<AndroidDevice>()
        for (usbDevice in newUsbDevices){
            if(oldDevices.find { it.toAndroidDevice()?.usbDevice == usbDevice } == null) {

                // Jade or UsbDeviceMapper
                (JadeUsbDevice.fromUsbDevice(deviceManager = this, usbDevice = usbDevice)
                    ?: deviceMapper.invoke(this, usbDevice, null, null, null, null,null))?.let {
                    newDevices += it
                }
            }
        }

        usbDevices.value = oldDevices + newDevices
    }

    fun scanNfcDevices() {
        logger.i { "Scan for NFC devices" }

        val cardManager = NfcCardManager()
        cardManager.setCardListener(this)
        cardManager.start()

        val activity = activityProvider.getCurrentActivity()

        nfcAdapter?.enableReaderMode(
            activity,
            cardManager,
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null
        )
    }

    override fun onConnected(channel: CardChannel) {
        logger.i { "SATODEBUG DeviceManagerAndroid onConnected" }
        for (nfcDeviceType in NfcDeviceType.entries) {
            try {
                val cmdSet = SatochipCommandSet(channel)

                // try to select applet according to device candidate
                cmdSet.cardSelect(nfcDeviceType).checkOK()

                // device found, create new device
                val nfcDevice = NfcDevice(nfcDeviceType)

                // for Satochip devices, get card status
                if (nfcDeviceType == NfcDeviceType.SATOCHIP){
                    val statusApdu = cmdSet.satochipGetStatus().checkOK()

                    val statusBytes = statusApdu.getData()
                    val statusSize = statusBytes.size
                    logger.i { "SATODEBUG DeviceManagerAndroid onConnected: statusSize: $statusSize" }

                    // check if card is seeded
                    val isSeeded = if ((statusBytes[9].toInt() == 0X00)) false else true
                    nfcDevice.isSeeded = isSeeded
                    logger.i { "SATODEBUG DeviceManagerAndroid onConnected: isSeeded: $isSeeded" }

                    // check that 2FA is not enabled
                    val needs2FA = if ((statusBytes[8].toInt() == 0X00)) false else true
                    logger.i { "SATODEBUG DeviceManagerAndroid onConnected: needs2FA: $needs2FA" }

                    // check if Liquid is supported
                    val supportsLiquid = if ((statusSize>15) && (statusBytes[15].toInt() == 0X00)) true else false
                    nfcDevice.supportsLiquid = supportsLiquid
                    logger.i { "SATODEBUG DeviceManagerAndroid onConnected: supportsLiquid: $supportsLiquid" }
                }

                // add device
                val newDevices = mutableListOf<AndroidDevice>()
                deviceMapper.invoke(this, null, null, null, null, nfcDevice, activityProvider)
                    ?.let {
                        newDevices += it
                    }
                logger.i { "SATODEBUG DeviceManagerAndroid onConnected newDevices: ${newDevices}" }
                nfcDevices.value = newDevices

                // disconnect card
                onDisconnected()

                // stop polling
                val activity = activityProvider.getCurrentActivity()
                nfcAdapter?.disableReaderMode(activity)

                // should probably avoid to connect to multiple NFC devices at the same time?
                return
            } catch (e: Exception) {
                logger.i { "SATODEBUG DeviceManagerAndroid onConnected: failed to connect to device: $nfcDeviceType" }
            }
        }
    }

    override fun onDisconnected() {
        logger.i { "SATODEBUG DeviceManagerAndroid onDisconnected: Card disconnected!" }
    }


    companion object : Loggable() {
        private const val ACTION_USB_PERMISSION = "com.blockstream.green.USB_PERMISSION"
    }
}