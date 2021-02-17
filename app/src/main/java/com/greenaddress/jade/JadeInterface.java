package com.greenaddress.jade;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.polidea.rxandroidble2.RxBleDevice;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Mid-level interface to Jade
 * Wraps either a serial or a ble connection
 * Calls to send and receive messages as JsonNode trees.
 * Intended for use wrapped by JadeAPI (see JadeAPI.createSerial() and JadeAPI.createBle()).
 */
class JadeInterface {
    private final static String TAG = "JadeInterface";
    private final static String HWTAG = "JadeInterface-hw";

    // The object mapper used to crease/parse the serialised format
    private static final ObjectMapper objectMapper = new ObjectMapper(new CBORFactory());

    private final JadeConnectionImpl connection;

    private JadeInterface(final JadeConnectionImpl connection) {
        this.connection = connection;
    }

    public static JadeInterface createSerial(final UsbManager usbManager, final UsbDevice usbDevice, final int baud) {
        final JadeSerialImpl serial = new JadeSerialImpl(usbManager, usbDevice, baud);
        return new JadeInterface(serial);
    }

    public static JadeInterface createBle(final RxBleDevice device) {
        final JadeBleImpl ble = new JadeBleImpl(device);
        return new JadeInterface(ble);
    }

    public static ObjectMapper mapper() {
        return objectMapper;
    }

    public boolean isConnected() {
        return this.connection.isConnected();
    }

    public void connect() {
        this.connection.connect();

        // Read and discard anything present on connection
        final byte[] bytes = this.drain();
        Log.d(TAG, "Discarded " + bytes.length + " bytes on connection");
    }

    public void disconnect() {
        this.connection.disconnect();
    }

    public void writeRequest(final JsonNode request) throws IOException {
        if (!isConnected()) {
            throw new IOException("JadeInterface not connected");
        }

        Log.i(TAG, "Sending request:" + request);
        final byte[] bytes = mapper().writeValueAsBytes(request);
        Log.d(TAG, "Sending " + bytes.length + " bytes");
        this.connection.write(bytes);
    }

    public byte[] drain() {
        Log.d(TAG, "Draining interface");
        return this.connection.drain();
    }

    public JsonNode readResponse(final int timeout) throws IOException {
        if (!isConnected()) {
            throw new IOException("JadeInterface not connected");
        }

        Log.d(TAG, "Awaiting response - timeout(ms): " + timeout);
        final ByteArrayOutputStream collected = new ByteArrayOutputStream(256);
        while (true) {
            // Collect response bytes so we can try to parse them as a cbor message
            final Byte next = this.connection.read(timeout);
            if (next == null) {
                // Timeout or other critical error
                Log.w(TAG, "read() operation returned no next byte - timeout(ms): " + timeout);
                return null;
            }
            collected.write(next);

            try {
                // Try to parse as into response message
                final JsonNode response = mapper().readTree(collected.toByteArray());

                // Is it a response to a request ?
                if (response.has("id")) {
                    // A proper response
                    Log.i(TAG, "Response received: " + response);
                    return response;
                }

                // Is it a log message ?
                if (response.has("log")) {
                    // A log message
                    final String logmsg = new String(response.get("log").binaryValue());
                    if (logmsg.length() > 1 && logmsg.charAt(1) == ' ') {
                        switch (logmsg.charAt(0)) {
                            case 'V':
                                Log.v(HWTAG, logmsg);
                                break;
                            case 'D':
                                Log.d(HWTAG, logmsg);
                                break;
                            case 'I':
                                Log.i(HWTAG, logmsg);
                                break;
                            case 'W':
                                Log.w(HWTAG, logmsg);
                                break;
                            case 'E':
                                Log.e(HWTAG, logmsg);
                                break;
                            default:
                                Log.e(HWTAG, "Unrecognised log level: " + logmsg);
                        }
                    } else {
                        Log.e(TAG, "Unrecognised log message: " + logmsg);
                    }
                } else {
                    // Unknown message
                    Log.e(TAG, "Unrecognised message received - discarding: " + response);
                }

                // Clear the collected bytes
                collected.reset();
            } catch (final JsonParseException | MismatchedInputException | NullPointerException e) {
                // Not yet a parsable message - wait for more data
                // Log.w(TAG, e.getMessage());
            } catch (final Exception e) {
                Log.w(TAG, "Error when attempting to parse cbor message: " + e.getClass().getName());
                Log.w(TAG, "Error: " + e.getMessage());
                //e.printStackTrace();
            }
            // TODO: discard if 'collected' gets too big ?
        }
    }
/*
    // Version using input stream and not using parseBytes() above
    public JsonNode readResponseX(final int timeout) throws IOException {
        Log.d(TAG, "Awaiting response - timeout(ms): " + timeout);
        while (true) {
            final JsonNode response = objectMapper.readTree(this.connection.getInputStream());
            if (response.has("id")) {
                // A proper response
                Log.i(TAG, "Response received: " + response);
                return response;
            }
            if (response.has("log")) {
                // A log message
                final String logmsg = new String(response.get("log").binaryValue());
                if (logmsg.length() > 1 && logmsg.charAt(1) == ' ') {
                    switch (logmsg.charAt(0)) {
                        case 'V':
                            Log.v(HWTAG, logmsg);
                            break;
                        case 'D':
                            Log.d(HWTAG, logmsg);
                            break;
                        case 'I':
                            Log.i(HWTAG, logmsg);
                            break;
                        case 'W':
                            Log.w(HWTAG, logmsg);
                            break;
                        case 'E':
                            Log.e(HWTAG, logmsg);
                            break;
                        default:
                            Log.e(HWTAG, "Unrecognised log level: " + logmsg);
                    }
                } else {
                    Log.e(TAG, "Unrecognised log message: " + logmsg);
                }
            } else {
                // Unknown message
                Log.e(TAG, "Unrecognised message received - discarding: " + response.asText());
            }
        }
    }
*/
    public final JsonNode makeRpcCall(final JsonNode request, final int timeout, final boolean drain) throws IOException {
        if (JadeAPI.isDebug) {
            // Sanity check json-rpc request
            final JsonNode id = request.get("id");
            final JsonNode method = request.get("method");
            if (id == null || id.asText().length() == 0 || id.asText().length() >= 16 ||
                method == null || method.asText().length() == 0 || method.asText().length() >= 32) {
                throw new IllegalArgumentException("Invalid request: " + request);
            }
        }

        // If requested, drain any existing outstanding messages first
        if (drain) {
            this.drain();
        }

        // Send the request
        this.writeRequest(request);

        // Await the response
        final JsonNode response = readResponse(timeout);

        if (JadeAPI.isDebug && response != null) {
            // Sanity check json-rpc response
            final JsonNode id = response.get("id");
            final JsonNode result = response.get("result");
            final JsonNode error = response.get("error");

            if (id == null || id.asText().length() == 0 || id.asText().length() >= 16 ||
                (id.asText().equals("00") && (error == null)) ||
                (result == null) == (error == null)) {
                throw new IllegalArgumentException("Invalid response: " + response);
            }
        }

        return response;
    }
}
