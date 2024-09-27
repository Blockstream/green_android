package com.blockstream.jade.connection

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.blockstream.jade.Loggable
import com.blockstream.jade.data.JadeError
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


/**
 * Low-level Serial backend interface to Jade
 * Calls to send and receive bytes to/from Jade.
 * Intended for use wrapped by JadeInterface (see JadeInterface.createSerial()).
 */
class JadeUsbConnection(
    private val usbDevice: UsbDevice,
    private val usbManager: UsbManager
) : JadeConnection() {

    private var usbSerialDevice: UsbSerialDevice? = null

    override val isUsb: Boolean = true

    override val isConnected: Boolean
        get() = usbSerialDevice?.isOpen == true

    private val _disconnectEvent = MutableStateFlow(true)

    override val disconnectEvent: StateFlow<Boolean>
        get() = _disconnectEvent.asStateFlow()

    @Throws
    override suspend fun connect() {
        // Maybe collapse this into the ctor ?  Maybe see how BLE pans out.
        usbSerialDevice = usbManager.openDevice(usbDevice)
            .let { UsbSerialDevice.createUsbSerialDevice(usbDevice, it) }

        if (usbSerialDevice == null || usbSerialDevice?.open() == false) {
            throw JadeError(1, "Failed to connect over usb/serial", null)
        }

        usbSerialDevice?.apply {
            _disconnectEvent.value = false
            // Set params
            setBaudRate(BAUD_RATE)
            setDataBits(UsbSerialInterface.DATA_BITS_8)
            setStopBits(UsbSerialInterface.STOP_BITS_1)
            setParity(UsbSerialInterface.PARITY_NONE)
            setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF)
            // Set the callback for receiving data over the serial connection
            // (Just collect into base-class queue of byte-arrays.)
            read(::onDataReceived)
        }
    }

    override suspend fun write(bytes: ByteArray): Int {
        usbSerialDevice?.write(bytes)
        return bytes.size
    }

    override suspend fun disconnect() {
        _disconnectEvent.value = true
        usbSerialDevice?.close()
        usbSerialDevice = null
    }

    companion object : Loggable() {
        private const val BAUD_RATE = 115200
    }
}