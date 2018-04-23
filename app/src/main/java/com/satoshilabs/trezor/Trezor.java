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
import android.util.Pair;

import com.blockstream.libwally.Wally;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.greenaddress.greenapi.GATx;
import com.greenaddress.greenapi.HDClientKey;
import com.greenaddress.greenapi.HDKey;
import com.greenaddress.greenapi.ISigningWallet;
import com.greenaddress.greenapi.Network;
import com.greenaddress.greenapi.Output;
import com.greenaddress.greenapi.PreparedTransaction;
import com.greenaddress.greenbits.GaService;
import com.satoshilabs.trezor.protobuf.TrezorMessage.Address;
import com.satoshilabs.trezor.protobuf.TrezorMessage.ButtonAck;
import com.satoshilabs.trezor.protobuf.TrezorMessage.ButtonRequest;
import com.satoshilabs.trezor.protobuf.TrezorMessage.Entropy;
import com.satoshilabs.trezor.protobuf.TrezorMessage.EntropyRequest;
import com.satoshilabs.trezor.protobuf.TrezorMessage.Failure;
import com.satoshilabs.trezor.protobuf.TrezorMessage.Features;
import com.satoshilabs.trezor.protobuf.TrezorMessage.GetPublicKey;
import com.satoshilabs.trezor.protobuf.TrezorMessage.Initialize;
import com.satoshilabs.trezor.protobuf.TrezorMessage.MessageSignature;
import com.satoshilabs.trezor.protobuf.TrezorMessage.MessageType;
import com.satoshilabs.trezor.protobuf.TrezorMessage.PassphraseAck;
import com.satoshilabs.trezor.protobuf.TrezorMessage.PassphraseRequest;
import com.satoshilabs.trezor.protobuf.TrezorMessage.PinMatrixAck;
import com.satoshilabs.trezor.protobuf.TrezorMessage.PinMatrixRequest;
import com.satoshilabs.trezor.protobuf.TrezorMessage.PublicKey;
import com.satoshilabs.trezor.protobuf.TrezorMessage.SignMessage;
import com.satoshilabs.trezor.protobuf.TrezorMessage.SignTx;
import com.satoshilabs.trezor.protobuf.TrezorMessage.Success;
import com.satoshilabs.trezor.protobuf.TrezorMessage.TxAck;
import com.satoshilabs.trezor.protobuf.TrezorMessage.TxRequest;
import com.satoshilabs.trezor.protobuf.TrezorMessage.TxSize;
import com.satoshilabs.trezor.protobuf.TrezorMessage.WordRequest;
import com.satoshilabs.trezor.protobuf.TrezorType.HDNodePathType;
import com.satoshilabs.trezor.protobuf.TrezorType.HDNodeType;
import com.satoshilabs.trezor.protobuf.TrezorType.InputScriptType;
import com.satoshilabs.trezor.protobuf.TrezorType.MultisigRedeemScriptType;
import com.satoshilabs.trezor.protobuf.TrezorType.OutputScriptType;
import com.satoshilabs.trezor.protobuf.TrezorType.RequestType;
import com.satoshilabs.trezor.protobuf.TrezorType.TransactionType;
import com.satoshilabs.trezor.protobuf.TrezorType.TxInputType;
import com.satoshilabs.trezor.protobuf.TrezorType.TxOutputBinType;
import com.satoshilabs.trezor.protobuf.TrezorType.TxOutputType;
import com.satoshilabs.trezor.protobuf.TrezorType.TxRequestDetailsType;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.WrongNetworkException;
import org.bitcoinj.crypto.DeterministicKey;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


public class Trezor {

    private static final String TAG = Trezor.class.getSimpleName();
    private static final NetworkParameters NETWORK = Network.NETWORK;

    private final int mVendorId;
    private final UsbDeviceConnection mConn;
    private final String mSerial;
    private final UsbEndpoint mReadEndpoint, mWriteEndpoint;
    private final TrezorGUICallback mGuiFn;

    private PreparedTransaction mTx;
    private org.bitcoinj.core.Address mChangeAddress;
    private HDNodeType mGAKey, mUserKey, mBackupKey;
    private final ArrayList<String> mSignatures = new ArrayList<>();

    public static Trezor getDevice(final Context context, final TrezorGUICallback guiFn) {
        final UsbManager manager = (UsbManager)context.getSystemService(Context.USB_SERVICE);

        for (final UsbDevice device: manager.getDeviceList().values()) {
            // Check if the device is TREZOR (or AvalonWallet or BWALLET)

            if ((device.getVendorId() != 0x534c || device.getProductId() != 0x0001) &&
                    (device.getVendorId() != 0x10c4 || device.getProductId() != 0xea80)) {
                continue;
            }

            Log.i(TAG, "Hardware Wallet device found");
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
                Log.e(TAG, String.format("ep %d", ep.getAddress()));
            }

            if (!isEndpointOK(readEndpoint, "read") || !isEndpointOK(writeEndpoint, "write"))
                continue;

            // Try to open the device
            final UsbDeviceConnection conn = manager.openDevice(device);
            if (conn == null || !conn.claimInterface(iface,  true)) {
                Log.e(TAG, conn == null ? "Could not open connection" : "Could not claim interface");
                continue;
            }

            // All OK - return the class
            return new Trezor(guiFn, device, conn, readEndpoint, writeEndpoint);
        }
        return null;
    }

    private static boolean isEndpointOK(final UsbEndpoint endpoint, final String type) {
        if (endpoint != null && endpoint.getMaxPacketSize() == 64)
            return true;
        Log.e(TAG, type + " endpoint error: " + (endpoint == null ? "not found" : "bad packet size"));
        return false;
    }

    private Trezor(final TrezorGUICallback guiFn, final UsbDevice device,
                   final UsbDeviceConnection conn,
                   final UsbEndpoint readEndpoint, final UsbEndpoint writeEndpoint) {
        mGuiFn = guiFn;
        mVendorId = device.getVendorId();
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

    private Integer getChangePointer() {
        return mTx.mChangeOutput == null ? null : mTx.mChangeOutput.mPointer;
    }

    private static String b2h(final byte[] bytes) {
        return Wally.hex_from_bytes(bytes);
    }

    private static byte[] h2b(final String hex) {
        return Wally.hex_to_bytes(hex);
    }

    private Transaction getPreviousTx(final TxRequestDetailsType txRequest) {
        return mTx.mPrevoutRawTxs.get(b2h(txRequest.getTxHash().toByteArray()));
    }

    private HDNodeType makeHDKey(final DeterministicKey k) {
        // Depth seems only used to ensure we don't derive > 255 deep.
        return HDNodeType.newBuilder().setDepth(1)
                         .setFingerprint(0).setChildNum(k.getChildNumber().getI())
                         .setPublicKey(ByteString.copyFrom(k.getPubKey()))
                         .setChainCode(ByteString.copyFrom(k.getChainCode())).build();
    }

    private HDNodePathType makeHDNode(final HDNodeType node, final Integer pointer) {
        return HDNodePathType.newBuilder().setNode(node)
                             .clearAddressN().addAddressN(pointer).build();
    }

    private List<Integer> makePath(final Integer pointer) {
        final List<Integer> path = new ArrayList<>(4);
        if (mTx.mSubAccount != 0) {
            path.add(3 + 0x80000000);
            path.add(mTx.mSubAccount + 0x80000000);
        }
        path.add(HDKey.BRANCH_REGULAR);
        if (pointer != null)
            path.add(pointer);
        return path;
    }

    private void logData(final String prefix, final byte[] data) {
        String s = prefix;
        for (final byte b : data)
            s += String.format(" %02x", b);
        Log.i(TAG, s);
    }

    private void messageWrite(final Message msg) {
        final int msg_size = msg.getSerializedSize();
        final String msg_name = msg.getClass().getSimpleName();
        final int msg_id = MessageType.valueOf("MessageType_" + msg_name).getNumber();
        Log.i(TAG, String.format("Got message: %s (%d bytes)", msg_name, msg_size));
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
        Log.i(TAG, String.format("Writing %d chunks", chunks));
        data.rewind();
        for (int i = 0; i < chunks; ++i) {
            final byte[] buffer = new byte[64];
            buffer[0] = (byte)'?';
            data.get(buffer, 1, 63);
            logData("chunk:", buffer);
            request.queue(ByteBuffer.wrap(buffer), 64);
            mConn.requestWait();
        }
        request.close();
    }

    private Message parseMessageFromBytes(final MessageType type, final byte[] data) {
        Log.i(TAG, String.format("Parsing %s (%d bytes):", type, data.length));
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
                case MessageType.MessageType_TxSize_VALUE: return TxSize.parseFrom(data);
                case MessageType.MessageType_WordRequest_VALUE: return WordRequest.parseFrom(data);
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
            Log.i(TAG, String.format("Read chunk: %d bytes", b.length));
            if (b.length < 9) continue;
            logData("read:", b);

            final int rem = cur63.remaining(), len = b[0]&0xFF;
            cur63.put(b, 1, Math.min(len, rem));
            if (cur63.position() >= 63) {
                final byte[] b2 = cur63.array();
                if (b2[0] != (byte) '#' || b2[1] != (byte) '#') continue;
                type = MessageType.valueOf((b2[2] << 8) + b2[3]);
                msg_size = ((b2[4] & 0xFF) << 24) + ((b2[5] & 0xFF) << 16) + ((b2[6] & 0xFF) << 8) + (b2[7] & 0xFF);
                Log.i(TAG, String.format("msg_size: %d bytes", msg_size));
                data.put(b2, 8, cur63.position() - 8);
                if (rem < len) data.put(b, rem + 1, len - rem);
                break;
            }
        }
        while (data.position() < msg_size) {
            request.queue(buffer, 64);
            mConn.requestWait();
            final byte[] b = buffer.array();
            Log.i(TAG, String.format("Read chunk (cont): %d bytes msg size %d", b.length, msg_size));
            logData("read(cont):", b);
            data.put(b, 1, b[0] & 0xFF);
        }
        request.close();
        return parseMessageFromBytes(type, Arrays.copyOfRange(data.array(), 0, msg_size));
    }

    private String io(final Message.Builder builder) {
        messageWrite(builder.build());
        return _get(messageRead());
    }

    private String ioTx(final TransactionType.Builder builder) {
        return io(TxAck.newBuilder().setTx(builder));
    }

    private String _get(final Message resp) {
        switch (resp.getClass().getSimpleName()) {
        case "Features": {
            final Features r = (Features) resp;
            return r.getMajorVersion() + "." + r.getMinorVersion() + "." + r.getPatchVersion();
        }
        case "Success":
            return ((Success) resp).hasMessage() ? ((Success) resp).getMessage() : "";
        case "Failure":
            Log.e(TAG, "Failure response");
            throw new IllegalStateException();
        /* User can catch ButtonRequest to Cancel by not calling _get */
        case "ButtonRequest":
            return io(ButtonAck.newBuilder());
        case "PinMatrixRequest":
            return io(PinMatrixAck.newBuilder().setPin(mGuiFn.pinMatrixRequest()));
        case "PassphraseRequest":
            /* TODO: UTF8 VS Unicode... Fight! */
            return io(PassphraseAck.newBuilder().setPassphrase(mGuiFn.passphraseRequest()));
        case "PublicKey": {
            final PublicKey r = (PublicKey) resp;
            if (!r.hasNode())
                throw new IllegalArgumentException();
            final HDNodeType N = r.getNode();
            final String NodeStr = (N.hasDepth() ? N.getDepth() : "") + "%" +
                (N.hasFingerprint() ? N.getFingerprint() : "") + "%" +
                (N.hasChildNum() ? N.getChildNum() : "") + "%" +
                (N.hasChainCode() ? b2h(N.getChainCode().toByteArray()) : "") + "%" +
                (N.hasPrivateKey() ? b2h(N.getPrivateKey().toByteArray()) : "") + "%" +
                (N.hasPublicKey() ? b2h(N.getPublicKey().toByteArray()) : "") + "%" +
                "";
            if (r.hasXpub())
                return NodeStr + ":!:" + r.getXpub() + ":!:" + b2h(r.getXpubBytes().toByteArray());
            return NodeStr;
        }
        case "MessageSignature":
            return b2h(((MessageSignature) resp).getSignature().toByteArray());
        case "TxRequest": {
            final TxRequest r = (TxRequest) resp;
            if (r.getSerialized() != null && r.getSerialized().hasSignatureIndex())
                mSignatures.set(r.getSerialized().getSignatureIndex(),
                                b2h(r.getSerialized().getSignature().toByteArray()));

            if (r.getRequestType().equals(RequestType.TXFINISHED))
                return Joiner.on(";").join(mSignatures);

            final TxRequestDetailsType txRequest = r.getDetails();
            TransactionType.Builder ackTx = TransactionType.newBuilder();

            if (r.getRequestType().equals(RequestType.TXINPUT))
                return ioTx(ackTx.clearInputs().addInputs(createInput(txRequest)));

            if (r.getRequestType().equals(RequestType.TXOUTPUT)) {
                if (txRequest.hasTxHash())
                    return ioTx(ackTx.clearOutputs().addBinOutputs(createBinOutput(txRequest)));

                return ioTx(ackTx.clearOutputs()
                                 .addOutputs(createOutput(txRequest.getRequestIndex())));
            }

            if (r.getRequestType().equals(RequestType.TXMETA)) {
                final Transaction tx;
                tx = txRequest.hasTxHash() ? getPreviousTx(txRequest) : mTx.mDecoded;
                return ioTx(ackTx.setInputsCnt(tx.getInputs().size())
                                 .setOutputsCnt(tx.getOutputs().size())
                                 .setVersion((int) tx.getVersion())
                                 .setLockTime((int) tx.getLockTime()));
            }
            break; // Fall through
        }
        }
        return resp.getClass().getSimpleName();
    }

    private MultisigRedeemScriptType makeRedeemScript(final Integer pointer) {
        MultisigRedeemScriptType.Builder multisig;
        multisig = MultisigRedeemScriptType.newBuilder()
                                           .clearPubkeys()
                                           .addPubkeys(makeHDNode(mGAKey, pointer))
                                           .addPubkeys(makeHDNode(mUserKey, pointer));
        if (mBackupKey != null)
            multisig.addPubkeys(makeHDNode(mBackupKey, pointer)); // 2of 3
        return multisig.setM(2).build();
    }

    private TxOutputType.Builder createOutput(final int index) {
        final TransactionOutput txOut = mTx.mDecoded.getOutputs().get(index);

        final TxOutputType.Builder txout;
        txout = TxOutputType.newBuilder().setAmount(txOut.getValue().longValue());

        if (!txOut.getScriptPubKey().isPayToScriptHash()) {
            // p2pkh output
            return txout.setAddress(txOut.getAddressFromP2PKHScript(NETWORK).toString())
                        .setScriptType(OutputScriptType.PAYTOADDRESS);
        }

        // p2sh
        if (getChangePointer() == null ||
                !txOut.getAddressFromP2SH(NETWORK).equals(mChangeAddress)) {
            // p2sh non-change output
            return txout.setScriptType(OutputScriptType.PAYTOSCRIPTHASH)
                        .setAddress(txOut.getAddressFromP2SH(NETWORK).toString());
        }

        Log.d(TAG, "Matched change address: " + txOut.getAddressFromP2SH(NETWORK).toString());
        if (mTx.mChangeOutput.mIsSegwit) {
            // p2sh-p2wsh change output
            txout.setScriptType(OutputScriptType.PAYTOP2SHWITNESS);
        } else {
            // p2sh change output
            txout.setScriptType(OutputScriptType.PAYTOMULTISIG);
        }
        return txout.clearAddressN().addAllAddressN(makePath(getChangePointer()))
                    .setMultisig(makeRedeemScript(getChangePointer()));
    }

    private TxOutputBinType.Builder createBinOutput(final TxRequestDetailsType txRequest) {
        final int index = txRequest.getRequestIndex();

        final TransactionOutput out = getPreviousTx(txRequest).getOutput(index);
        return TxOutputBinType.newBuilder()
                              .setAmount(out.getValue().longValue())
                              .setScriptPubkey(ByteString.copyFrom(out.getScriptBytes()));
    }

    private TxInputType.Builder createInput(final TxRequestDetailsType txRequest) {
        final int index = txRequest.getRequestIndex();

        final TransactionInput in;
        in = (txRequest.hasTxHash() ? getPreviousTx(txRequest) : mTx.mDecoded).getInput(index);

        TxInputType.Builder txin;
        txin = TxInputType.newBuilder()
                          .setPrevHash(ByteString.copyFrom(in.getOutpoint().getHash().getBytes()))
                          .setPrevIndex((int) in.getOutpoint().getIndex())
                          .setSequence((int) in.getSequenceNumber());

        if (txRequest.hasTxHash())
            return txin.setScriptSig(ByteString.copyFrom(in.getScriptBytes()));

        final Output prevout = mTx.mPrevOutputs.get(index);
        txin.clearAddressN().addAllAddressN(makePath(prevout.pointer))
                            .setMultisig(makeRedeemScript(prevout.pointer));

        if (prevout.scriptType.equals(GATx.P2SH_P2WSH_FORTIFIED_OUT))
            return txin.setScriptType(InputScriptType.SPENDP2SHWITNESS)
                       .setAmount(prevout.value);
        return txin.setScriptType(InputScriptType.SPENDMULTISIG);
    }

    public Pair<byte[], byte[]> getUserKey(final List<Integer> path) {
        final String xpub = io(GetPublicKey.newBuilder().clearAddressN().addAllAddressN(path));
        final String[] data = xpub.split("%", -1);
        // Pubkey, Chaincode
        return new Pair<>(h2b(data[data.length - 2]), h2b(data[data.length - 4]));
    }

    public ECKey.ECDSASignature signMessage(final List<Integer> path, final String message) {
        final byte[] sig;
        sig = h2b(io(SignMessage.newBuilder()
                                .clearAddressN().addAllAddressN(path)
                                .setMessage(ByteString.copyFromUtf8(message))));
        return new ECKey.ECDSASignature(new BigInteger(1, Arrays.copyOfRange(sig, 1, 33)),
                                        new BigInteger(1, Arrays.copyOfRange(sig, 33, 65)));
    }

    public List<Integer> getFirmwareVersion() {
        LinkedList<Integer> versionParts = new LinkedList<>();
        for (final String s : Splitter.on(".").split(io(Initialize.newBuilder())))
            versionParts.add(Integer.valueOf(s));
        return versionParts;
    }

    public List<byte[]> signTransaction(final PreparedTransaction ptx, final String coinName) {
        mTx = ptx;

        mGAKey = makeHDKey(HDKey.getGAPublicKeys(ptx.mSubAccount, null)[0]);

        mBackupKey = null;
        if (ptx.mTwoOfThreeBackupChaincode != null)
            mBackupKey = makeHDKey(HDKey.getRecoveryKeys(ptx.mTwoOfThreeBackupChaincode,
                                                         ptx.mTwoOfThreeBackupPubkey, null)[0]);

        mChangeAddress = null;
        if (getChangePointer() != null) {
            byte[] script = GaService.createOutScript(ptx.mSubAccount, getChangePointer(),
                                                      ptx.mTwoOfThreeBackupPubkey,
                                                      ptx.mTwoOfThreeBackupChaincode);
            try {
                if (mTx.mChangeOutput.mIsSegwit)
                    script = GaService.getSegWitScript(script);
                mChangeAddress = new org.bitcoinj.core.Address(NETWORK, NETWORK.getP2SHHeader(),
                                                               Wally.hash160(script));
                Log.d(TAG, "Change address: " + mChangeAddress.toString());
            } catch (final WrongNetworkException e) {
            }
        }

        mUserKey = makeHDKey(HDClientKey.getMyPublicKey(ptx.mSubAccount, null));

        final int numInputs = ptx.mDecoded.getInputs().size();
        final int numOutputs = ptx.mDecoded.getOutputs().size();

        mSignatures.clear();
        for (int i = 0; i < numInputs; ++i)
            mSignatures.add("");

        final String[] sigs;
        sigs = io(SignTx.newBuilder().setInputsCount(numInputs)
                                     .setOutputsCount(numOutputs)
                                     .setCoinName(coinName)
                                     .setLockTime((int) ptx.mDecoded.getLockTime()))
                                     .split(";");

        final LinkedList<byte[]> signaturesList = new LinkedList<>();
        for (final String sig: sigs)
            signaturesList.add(ISigningWallet.getTxSignature(ECKey.ECDSASignature.decodeFromDER(h2b(sig))));

        return signaturesList;
    }
}
