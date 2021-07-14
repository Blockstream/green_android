package com.greenaddress.jade;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.greenaddress.jade.entities.JadeError;

/**
 * Low-level Serial backend interface to Jade
 * Calls to send and receive bytes to/from Jade.
 * Intended for use wrapped by JadeInterface (see JadeInterface.createSerial()).
 */
public class JadeSerialImpl extends JadeConnectionImpl {
    private final UsbManager usbManager;
    private final UsbDevice usbDevice;
    private int baud;

    private UsbSerialDevice serial;

    JadeSerialImpl(final UsbManager usbManager, final UsbDevice usbDevice, final int baud) {
        this.usbManager = usbManager;
        this.usbDevice = usbDevice;
        this.baud = baud;
        this.serial = null;
    }

    @Override
    public boolean isConnected() {
        return this.serial != null && this.serial.isOpen();
    }

    @Override
    public void connect() {
        // Maybe collapse this into the ctor ?  Maybe see how BLE pans out.
        final UsbDeviceConnection usbConnection = usbManager.openDevice(this.usbDevice);
        this.serial = UsbSerialDevice.createUsbSerialDevice(this.usbDevice, usbConnection);

        if (this.serial == null || !this.serial.open()) {
            throw new JadeError(1, "Failed to connect over usb/serial", null);
        }

        // Set params
        this.serial.setBaudRate(baud);
        this.serial.setDataBits(UsbSerialInterface.DATA_BITS_8);
        this.serial.setStopBits(UsbSerialInterface.STOP_BITS_1);
        this.serial.setParity(UsbSerialInterface.PARITY_NONE);
        this.serial.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);

        // Set the callback for receiving data over the serial connection
        // (Just collect into base-class queue of byte-arrays.)
        this.serial.read(super::onDataReceived);
    }

    @Override
    public void disconnect() {
        if (this.serial != null) {
            serial.close();
            serial = null;
        }
    }

    @Override
    public int write(final byte[] bytes) {
        this.serial.write(bytes);
        return bytes.length;
    }
}