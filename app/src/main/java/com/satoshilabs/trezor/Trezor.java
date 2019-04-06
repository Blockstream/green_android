package com.satoshilabs.trezor;

import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.util.Log;
import com.blockstream.libwally.Wally;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.satoshilabs.trezor.protobuf.TrezorMessage.Address;
import com.satoshilabs.trezor.protobuf.TrezorMessage.ButtonRequest;
import com.satoshilabs.trezor.protobuf.TrezorMessage.Entropy;
import com.satoshilabs.trezor.protobuf.TrezorMessage.EntropyRequest;
import com.satoshilabs.trezor.protobuf.TrezorMessage.Failure;
import com.satoshilabs.trezor.protobuf.TrezorMessage.Features;
import com.satoshilabs.trezor.protobuf.TrezorMessage.Initialize;
import com.satoshilabs.trezor.protobuf.TrezorMessage.MessageSignature;
import com.satoshilabs.trezor.protobuf.TrezorMessage.MessageType;
import com.satoshilabs.trezor.protobuf.TrezorMessage.PassphraseRequest;
import com.satoshilabs.trezor.protobuf.TrezorMessage.PassphraseStateRequest;
import com.satoshilabs.trezor.protobuf.TrezorMessage.PinMatrixRequest;
import com.satoshilabs.trezor.protobuf.TrezorMessage.PublicKey;
import com.satoshilabs.trezor.protobuf.TrezorMessage.Success;
import com.satoshilabs.trezor.protobuf.TrezorMessage.TxRequest;
import com.satoshilabs.trezor.protobuf.TrezorMessage.TxSize;
import com.satoshilabs.trezor.protobuf.TrezorMessage.WordRequest;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;


public class Trezor {

    private static final String TAG = Trezor.class.getSimpleName();

    private final int mVendorId;
    private final int mProductId;
    private final UsbDeviceConnection mConn;
    private final String mSerial;
    private final UsbEndpoint mReadEndpoint, mWriteEndpoint;

    public static Trezor getDevice(final Context context) {
        final UsbManager manager = (UsbManager)context.getSystemService(Context.USB_SERVICE);

        for (final UsbDevice device: manager.getDeviceList().values()) {
            // Check if the device is TREZOR (or AvalonWallet or BWALLET)

            final int vendorId = device.getVendorId();
            final int productId = device.getProductId();

            if ((vendorId != 0x534c || productId != 0x0001) &&
                (vendorId != 0x1209 || productId != 0x53c0) &&
                (vendorId != 0x1209 || productId != 0x53c1) &&
                (vendorId != 0x10c4 || productId != 0xea80)) {
                continue;
            }

            Log.d(TAG, "Hardware Wallet device found");
            if (device.getInterfaceCount() < 1) {
                Log.e(TAG, "Wrong interface count");
                continue;
            }

            // Use first interface
            final UsbInterface iface = device.getInterface(0);
            // Try to find read/write endpoints
            UsbEndpoint readEndpoint = null, writeEndpoint = null;

            for (int i = 0; i < iface.getEndpointCount(); ++i) {
                final UsbEndpoint ep = iface.getEndpoint(i);

                if (readEndpoint == null &&
                        ep.getType() == UsbConstants.USB_ENDPOINT_XFER_INT &&
                        ep.getAddress() == 0x81) {
                    // number = 1 ; dir = USB_DIR_IN
                    readEndpoint = ep;
                    continue;
                }
                if (writeEndpoint == null &&
                        ep.getType() == UsbConstants.USB_ENDPOINT_XFER_INT &&
                        (ep.getAddress() == 0x01 || ep.getAddress() == 0x02)) {
                    // number = 1 ; dir = USB_DIR_OUT
                    writeEndpoint = ep;
                    continue;
                }
                Log.d(TAG, String.format("ep %d", ep.getAddress()));
            }

            if (isEndpointBad(readEndpoint, "read") || isEndpointBad(writeEndpoint, "write"))
                continue;

            // Try to open the device
            final UsbDeviceConnection conn = manager.openDevice(device);
            if (conn == null || !conn.claimInterface(iface,  true)) {
                Log.e(TAG, conn == null ? "Could not open connection" : "Could not claim interface");
                continue;
            }

            // All OK - return the class
            return new Trezor(device, conn, readEndpoint, writeEndpoint);
        }
        return null;
    }

    private static boolean isEndpointBad(final UsbEndpoint endpoint, final String type) {
        if (endpoint != null && endpoint.getMaxPacketSize() == 64)
            return false;
        Log.e(TAG, type + " endpoint error: " + (endpoint == null ? "not found" : "bad packet size"));
        return true;
    }

    private Trezor(final UsbDevice device, final UsbDeviceConnection conn,
                   final UsbEndpoint readEndpoint, final UsbEndpoint writeEndpoint) {
        mVendorId = device.getVendorId();
        mProductId = device.getProductId();
        mConn = conn;
        mReadEndpoint = readEndpoint;
        mWriteEndpoint = writeEndpoint;
        mSerial = mConn.getSerial();
    }

    @Override
    public String toString() {
        return "TREZOR(#" + mSerial + ")";
    }

    public int getVendorId() {
        return mVendorId;
    }

    public int getProductId() {
        return mProductId;
    }

    private void logData(final String prefix, final byte[] data) {
        Log.d(TAG, prefix + Wally.hex_from_bytes(data));
    }

    private void messageWrite(final Message msg) {
        final int msg_size = msg.getSerializedSize();
        final String msg_name = msg.getClass().getSimpleName();
        final int msg_id = MessageType.valueOf("MessageType_" + msg_name).getNumber();
        Log.d(TAG, String.format("Got message: %s (%d bytes)", msg_name, msg_size));
        final ByteBuffer data = ByteBuffer.allocate(32768);
        data.put((byte)'#');
        data.put((byte)'#');
        data.put((byte)((msg_id >> 8) & 0xFF));
        data.put((byte)(msg_id & 0xFF));
        data.put((byte)((msg_size >> 24) & 0xFF));
        data.put((byte)((msg_size >> 16) & 0xFF));
        data.put((byte)((msg_size >> 8) & 0xFF));
        data.put((byte)(msg_size & 0xFF));
        data.put(msg.toByteArray());
        while (data.position() % 63 > 0)
            data.put((byte)0);
        final UsbRequest request = new UsbRequest();
        request.initialize(mConn, mWriteEndpoint);
        final int chunks = data.position() / 63;
        Log.d(TAG, String.format("Writing %d chunks", chunks));
        data.rewind();
        for (int i = 0; i < chunks; ++i) {
            final byte[] buffer = new byte[64];
            buffer[0] = (byte)'?';
            data.get(buffer, 1, 63);
            //logData("chunk:", buffer);
            request.queue(ByteBuffer.wrap(buffer), 64);
            mConn.requestWait();
        }
        request.close();
    }

    private Message parseMessageFromBytes(final MessageType type, final byte[] data) {
        Log.d(TAG, String.format("Parsing %s (%d bytes):", type, data.length));
        logData("data:", data);

        try {
            switch (type.getNumber()) {
                case MessageType.MessageType_Success_VALUE: return Success.parseFrom(data);
                case MessageType.MessageType_Failure_VALUE: return Failure.parseFrom(data);
                case MessageType.MessageType_Entropy_VALUE: return Entropy.parseFrom(data);
                case MessageType.MessageType_PublicKey_VALUE: return PublicKey.parseFrom(data);
                case MessageType.MessageType_Features_VALUE: return Features.parseFrom(data);
                case MessageType.MessageType_PinMatrixRequest_VALUE: return PinMatrixRequest.parseFrom(data);
                case MessageType.MessageType_TxRequest_VALUE: return TxRequest.parseFrom(data);
                case MessageType.MessageType_ButtonRequest_VALUE: return ButtonRequest.parseFrom(data);
                case MessageType.MessageType_Address_VALUE: return Address.parseFrom(data);
                case MessageType.MessageType_EntropyRequest_VALUE: return EntropyRequest.parseFrom(data);
                case MessageType.MessageType_MessageSignature_VALUE: return MessageSignature.parseFrom(data);
                case MessageType.MessageType_PassphraseRequest_VALUE: return PassphraseRequest.parseFrom(data);
                case MessageType.MessageType_PassphraseStateRequest_VALUE: return PassphraseStateRequest.parseFrom(data);
                case MessageType.MessageType_TxSize_VALUE: return TxSize.parseFrom(data);
                case MessageType.MessageType_WordRequest_VALUE: return WordRequest.parseFrom(data);

                default:
                    throw new InvalidProtocolBufferException(String.format("Unknown message type %s", type));
            }
        } catch (final InvalidProtocolBufferException e) {
            Log.e(TAG, e.toString());
        }
        return null;
    }

    private Message messageRead() {
        final ByteBuffer data = ByteBuffer.allocate(32768);
        final ByteBuffer buffer = ByteBuffer.allocate(64);
        final ByteBuffer cur63 = ByteBuffer.allocate(64);
        final UsbRequest request = new UsbRequest();
        request.initialize(mConn, mReadEndpoint);
        MessageType type;
        int msg_size;
        for (;;) {
            buffer.clear();
            if (!request.queue(buffer, 64)) continue;
            mConn.requestWait();
            final byte[] b = new byte[64];
            buffer.rewind();
            buffer.get(b);
            Log.d(TAG, String.format("Read chunk: %d bytes", b.length));
            if (b.length < 9) continue;
            //logData("read:", b);

            final int rem = cur63.remaining(), len = b[0]&0xFF;
            cur63.put(b, 1, Math.min(len, rem));
            if (cur63.position() >= 63) {
                final byte[] b2 = cur63.array();
                if (b2[0] != (byte) '#' || b2[1] != (byte) '#') continue;
                type = MessageType.valueOf((b2[2] << 8) + b2[3]);
                msg_size = ((b2[4] & 0xFF) << 24) + ((b2[5] & 0xFF) << 16) + ((b2[6] & 0xFF) << 8) + (b2[7] & 0xFF);
                Log.d(TAG, String.format("msg_size: %d bytes", msg_size));
                data.put(b2, 8, cur63.position() - 8);
                if (rem < len) data.put(b, rem + 1, len - rem);
                break;
            }
        }
        while (data.position() < msg_size) {
            request.queue(buffer, 64);
            mConn.requestWait();
            final byte[] b = buffer.array();
            //Log.d(TAG, String.format("Read chunk (cont): %d bytes msg size %d", b.length, msg_size));
            //logData("read(cont):", b);
            data.put(b, 1, b[0] & 0xFF);
        }
        request.close();
        return parseMessageFromBytes(type, Arrays.copyOfRange(data.array(), 0, msg_size));
    }

    public Message io(Message.Builder m) {
        messageWrite(m.build());
        return messageRead();
    }

    public List<Integer> getFirmwareVersion() {
        final Features r = (Features) io(Initialize.newBuilder());
        return ImmutableList.of(r.getMajorVersion(), r.getMinorVersion(),r.getPatchVersion());
    }
}
