package com.greenaddress.jade;

import java.util.UUID;

/**
 * Low-level BLE backend interface to Jade
 * Calls to send and receive bytes or text/json messages.
 * Intended for use wrapped by JadeInterface (see JadeInterface.createBle()).
 */
public class JadeBleImpl extends JadeConnectionImpl {
    private static final String TAG = "JadeBleImpl";

    public static final UUID IO_SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID IO_TX_CHAR_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID IO_RX_CHAR_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

    JadeBleImpl() {
        // FIXME: implement BLE
    }

    @Override
    public boolean isConnected() {
        // FIXME: implement BLE
        return false;
    }

    @Override
    public void connect() {
        // FIXME: implement BLE
    }

    @Override
    public void disconnect() {
        // FIXME: implement BLE
    }

    @Override
    public int write(final byte[] bytes) {
        // FIXME: implement BLE
        return 0;
    }
}
