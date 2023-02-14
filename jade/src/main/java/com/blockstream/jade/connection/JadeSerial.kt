package com.blockstream.jade.connection

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.blockstream.jade.JadeConnectionImpl
import com.blockstream.jade.entities.JadeError
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface


/**
 * Low-level Serial backend interface to Jade
 * Calls to send and receive bytes to/from Jade.
 * Intended for use wrapped by JadeInterface (see JadeInterface.createSerial()).
 */
class JadeSerial(private val usbManager: UsbManager, private val usbDevice: UsbDevice, private val baudRate: Int): JadeConnectionImpl() {

    private var usbSerialDevice: UsbSerialDevice? = null

    override fun isConnected(): Boolean = usbSerialDevice?.isOpen == true

    override fun connect() {

        // Maybe collapse this into the ctor ?  Maybe see how BLE pans out.
        usbSerialDevice = usbManager.openDevice(usbDevice).let { UsbSerialDevice.createUsbSerialDevice(usbDevice, it) }

        if(usbSerialDevice == null || usbSerialDevice?.open() == false){
            throw JadeError(1, "Failed to connect over usb/serial", null)
        }

        usbSerialDevice?.apply {
            // Set params
            setBaudRate(baudRate)
            setDataBits(UsbSerialInterface.DATA_BITS_8)
            setStopBits(UsbSerialInterface.STOP_BITS_1)
            setParity(UsbSerialInterface.PARITY_NONE)
            setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF)
            // Set the callback for receiving data over the serial connection
            // (Just collect into base-class queue of byte-arrays.)
            read(::onDataReceived)
        }
    }


    override fun write(bytes: ByteArray): Int {
        usbSerialDevice?.write(bytes)
        return bytes.size
    }

    override fun disconnect() {
        usbSerialDevice?.close()
        usbSerialDevice = null
    }
}