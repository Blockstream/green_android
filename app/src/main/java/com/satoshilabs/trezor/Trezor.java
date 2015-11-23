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

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.greenaddress.greenapi.Network;
import com.greenaddress.greenapi.PreparedTransaction;
import com.satoshilabs.trezor.protobuf.TrezorMessage;
import com.satoshilabs.trezor.protobuf.TrezorMessage.Address;
import com.satoshilabs.trezor.protobuf.TrezorMessage.ButtonRequest;
import com.satoshilabs.trezor.protobuf.TrezorMessage.Entropy;
import com.satoshilabs.trezor.protobuf.TrezorMessage.EntropyRequest;
import com.satoshilabs.trezor.protobuf.TrezorMessage.Failure;
import com.satoshilabs.trezor.protobuf.TrezorMessage.Features;
import com.satoshilabs.trezor.protobuf.TrezorMessage.MessageSignature;
import com.satoshilabs.trezor.protobuf.TrezorMessage.MessageType;
import com.satoshilabs.trezor.protobuf.TrezorMessage.PassphraseRequest;
import com.satoshilabs.trezor.protobuf.TrezorMessage.PinMatrixRequest;
import com.satoshilabs.trezor.protobuf.TrezorMessage.PublicKey;
import com.satoshilabs.trezor.protobuf.TrezorMessage.SignTx;
import com.satoshilabs.trezor.protobuf.TrezorMessage.Success;
import com.satoshilabs.trezor.protobuf.TrezorMessage.TxRequest;
import com.satoshilabs.trezor.protobuf.TrezorMessage.TxSize;
import com.satoshilabs.trezor.protobuf.TrezorMessage.WordRequest;
import com.satoshilabs.trezor.protobuf.TrezorType;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Utils;
import org.bitcoinj.core.WrongNetworkException;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.script.Script;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/* Stub for empty TrezorGUICallback */
class _TrezorGUICallback implements TrezorGUICallback {
	public String pinMatrixRequest() { return ""; }
	public String passphraseRequest() { return ""; }
}

public class Trezor {

    private PreparedTransaction curTx;
    private org.bitcoinj.core.Address curChangeAddr;
    private TrezorType.HDNodeType curGaNode, curWalletNode, curRecoveryNode;
    private int curSubaccount;
    ArrayList<String> curSignatures = new ArrayList<>();
	private static final String TAG = Trezor.class.getSimpleName();

    public static Trezor getDevice(Context context) {
		return getDevice(context,new _TrezorGUICallback());
	}

	public static Trezor getDevice(Context context, TrezorGUICallback guicall) {
		UsbManager manager = (UsbManager)context.getSystemService(Context.USB_SERVICE);
		HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
		Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

		while (deviceIterator.hasNext()) {
			UsbDevice device = deviceIterator.next();
			// check if the device is TREZOR (or AvalonWallet or BWALLET) or KeepKey
			if (((device.getVendorId() != 0x534c && device.getVendorId() != 0x2B24) || device.getProductId() != 0x0001) &&
                (device.getVendorId() != 0x10c4 || device.getProductId() != 0xea80)) {
				continue;
			}
			Log.i(TAG, "Hardware Wallet device found");
			if (device.getInterfaceCount() < 1) {
				Log.e(TAG, "Wrong interface count");
				continue;
			}
			// use first interface
			UsbInterface iface = device.getInterface(0);
			// try to find read/write endpoints
			UsbEndpoint epr = null, epw = null;
			for (int i = 0; i < iface.getEndpointCount(); i++) {
				UsbEndpoint ep = iface.getEndpoint(i);
				if (epr == null && ep.getType() == UsbConstants.USB_ENDPOINT_XFER_INT && ep.getAddress() == 0x81) { // number = 1 ; dir = USB_DIR_IN
					epr = ep;
					continue;
				}
				if (epw == null && ep.getType() == UsbConstants.USB_ENDPOINT_XFER_INT && (ep.getAddress() == 0x01 || ep.getAddress() == 0x02)) { // number = 1 ; dir = USB_DIR_OUT
					epw = ep;
					continue;
				}
                Log.e(TAG, String.format("ep %d", ep.getAddress()));
			}
			if (epr == null) {
				Log.e(TAG, "Could not find read endpoint");
				continue;
			}
			if (epw == null) {
				Log.e(TAG, "Could not find write endpoint");
				continue;
			}
			if (epr.getMaxPacketSize() != 64) {
				Log.e(TAG, "Wrong packet size for read endpoint");
				continue;
			}
			if (epw.getMaxPacketSize() != 64) {
				Log.e(TAG, "Wrong packet size for write endpoint");
				continue;
			}
			// try to open the device
			UsbDeviceConnection conn = manager.openDevice(device);
			if (conn == null) {
				Log.e(TAG, "Could not open connection");
				continue;
			}
			boolean claimed = conn.claimInterface(iface,  true);
			if (!claimed) {
				Log.e(TAG, "Could not claim interface");
				continue;
			}
			// all OK - return the class
			return new Trezor(guicall, device, conn, iface, epr, epw);
		}
		return null;
	}
	private int vendorId;
	private UsbDeviceConnection conn;
	private String serial;
	private UsbEndpoint epr, epw;
	private TrezorGUICallback guicall;
	public int getVendorId() {
                return vendorId;
        }
	public Trezor(TrezorGUICallback guicall, UsbDevice device, UsbDeviceConnection conn, UsbInterface iface, UsbEndpoint epr, UsbEndpoint epw) {
		this.guicall = guicall;
		this.vendorId = device.getVendorId();
		this.conn = conn;
		this.epr = epr;
		this.epw = epw;
		this.serial = this.conn.getSerial();
	}

	@Override
	public String toString() {
		return "TREZOR(#" + this.serial + ")";
	}

	private void messageWrite(Message msg) {
		int msg_size = msg.getSerializedSize();
		String msg_name = msg.getClass().getSimpleName();
		int msg_id = MessageType.valueOf("MessageType_" + msg_name).getNumber();
		Log.i(TAG, String.format("Got message: %s (%d bytes)", msg_name, msg_size));
		ByteBuffer data = ByteBuffer.allocate(32768);
		data.put((byte)'#');
		data.put((byte)'#');
		data.put((byte)((msg_id >> 8) & 0xFF));
		data.put((byte)(msg_id & 0xFF));
		data.put((byte)((msg_size >> 24) & 0xFF));
		data.put((byte)((msg_size >> 16) & 0xFF));
		data.put((byte)((msg_size >> 8) & 0xFF));
		data.put((byte)(msg_size & 0xFF));
		data.put(msg.toByteArray());
		while (data.position() % 63 > 0) {
			data.put((byte)0);
		}
		UsbRequest request = new UsbRequest();
		request.initialize(conn, epw);
		int chunks = data.position() / 63;
		Log.i(TAG, String.format("Writing %d chunks", chunks));
		data.rewind();
		for (int i = 0; i < chunks; i++) {
			byte[] buffer = new byte[64];
			buffer[0] = (byte)'?';
			data.get(buffer, 1, 63);
			String s = "chunk:";
			for (int j = 0; j < 64; j++) {
				s += String.format(" %02x", buffer[j]);
			}
			Log.i(TAG, s);
			request.queue(ByteBuffer.wrap(buffer), 64);
			conn.requestWait();
		}
	}

	private Message parseMessageFromBytes(MessageType type, byte[] data) {
		Message msg = null;
		Log.i(TAG, String.format("Parsing %s (%d bytes):", type, data.length));
		String s = "data:";
		for (int i = 0; i < data.length; i++) {
			s += String.format(" %02x", data[i]);
		}
		Log.i(TAG, s);
		try {
			if (type.getNumber() == MessageType.MessageType_Success_VALUE) msg = Success.parseFrom(data);
			if (type.getNumber() == MessageType.MessageType_Failure_VALUE) msg = Failure.parseFrom(data);
			if (type.getNumber() == MessageType.MessageType_Entropy_VALUE) msg = Entropy.parseFrom(data);
			if (type.getNumber() == MessageType.MessageType_PublicKey_VALUE) msg = PublicKey.parseFrom(data);
			if (type.getNumber() == MessageType.MessageType_Features_VALUE) msg = Features.parseFrom(data);
			if (type.getNumber() == MessageType.MessageType_PinMatrixRequest_VALUE) msg = PinMatrixRequest.parseFrom(data);
			if (type.getNumber() == MessageType.MessageType_TxRequest_VALUE) msg = TxRequest.parseFrom(data);
			if (type.getNumber() == MessageType.MessageType_ButtonRequest_VALUE) msg = ButtonRequest.parseFrom(data);
			if (type.getNumber() == MessageType.MessageType_Address_VALUE) msg = Address.parseFrom(data);
			if (type.getNumber() == MessageType.MessageType_EntropyRequest_VALUE) msg = EntropyRequest.parseFrom(data);
			if (type.getNumber() == MessageType.MessageType_MessageSignature_VALUE) msg = MessageSignature.parseFrom(data);
			if (type.getNumber() == MessageType.MessageType_PassphraseRequest_VALUE) msg = PassphraseRequest.parseFrom(data);
			if (type.getNumber() == MessageType.MessageType_TxSize_VALUE) msg = TxSize.parseFrom(data);
			if (type.getNumber() == MessageType.MessageType_WordRequest_VALUE) msg = WordRequest.parseFrom(data);
		} catch (InvalidProtocolBufferException e) {
			Log.e(TAG, e.toString());
			return null;
		}
		return msg;
	}

	private Message messageRead() {
		ByteBuffer data = ByteBuffer.allocate(32768);
		ByteBuffer buffer = ByteBuffer.allocate(64);
        ByteBuffer cur63 = ByteBuffer.allocate(64);
		UsbRequest request = new UsbRequest();
		request.initialize(conn, epr);
		MessageType type;
		int msg_size;
		for (;;) {
            buffer.clear();
			if (!request.queue(buffer, 64)) continue;
			conn.requestWait();
			byte[] b = new byte[64];
            buffer.rewind();
            buffer.get(b);
			Log.i(TAG, String.format("Read chunk: %d bytes", b.length));
			if (b.length < 9) continue;
            String s = "read:";
            for (int j = 0; j < b.length; j++) {
                s += String.format(" %02x", b[j]);
            }
            Log.i(TAG, s);
            int rem = cur63.remaining(), len = b[0]&0xFF;
            cur63.put(b, 1, Math.min(len, rem));
            if (cur63.position() >= 63) {
                byte[] b2 = cur63.array();
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
			conn.requestWait();
			byte[] b = buffer.array();
			Log.i(TAG, String.format("Read chunk (cont): %d bytes", b.length, msg_size));

            String s = "read(cont):";
            for (int j = 0; j < b.length; j++) {
                s += String.format(" %02x", b[j]);
            }
            Log.i(TAG, s);
			data.put(b, 1, b[0] & 0xFF);
		}
		return parseMessageFromBytes(type, Arrays.copyOfRange(data.array(), 0, msg_size));
	}

	public Message send(Message msg) {
		messageWrite(msg);
		return messageRead();
	}

	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	private static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for ( int j = 0; j < bytes.length; j++ ) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	private String _get(Message resp) {
		switch (resp.getClass().getSimpleName()) {
        case "Features": {
            TrezorMessage.Features r = (TrezorMessage.Features) resp;
            return r.getMajorVersion()+"."+r.getMinorVersion()+"."+r.getPatchVersion(); }
		case "Success": {
			TrezorMessage.Success r = (TrezorMessage.Success)resp;
			if(r.hasMessage())return r.getMessage();
			return ""; }
		case "Failure":
			throw new IllegalStateException();
		/* User can catch ButtonRequest to Cancel by not calling _get */
		case "ButtonRequest":
			return _get(this.send(TrezorMessage.ButtonAck.newBuilder().build()));
		case "PinMatrixRequest":
			return _get(this.send(
				TrezorMessage.PinMatrixAck.newBuilder().
				setPin(this.guicall.pinMatrixRequest()).
				build()));
		case "PassphraseRequest":
			return _get(this.send(
				TrezorMessage.PassphraseAck.newBuilder().
				/* TODO: UTF8 VS Unicode... Fight! */
				setPassphrase(this.guicall.passphraseRequest()).
				build()));
		case "PublicKey": {
			TrezorMessage.PublicKey r = (TrezorMessage.PublicKey)resp;
			if(!r.hasNode())throw new IllegalArgumentException();
			TrezorType.HDNodeType N = r.getNode();
			String NodeStr = ((N.hasDepth())?N.getDepth():"") +"%"+
				((N.hasFingerprint())?N.getFingerprint():"") +"%"+
				((N.hasChildNum())?N.getChildNum():"") +"%"+
				((N.hasChainCode())?bytesToHex(N.getChainCode().toByteArray()):"") +"%"+
				((N.hasPrivateKey())?bytesToHex(N.getPrivateKey().toByteArray()):"") +"%"+
				((N.hasPublicKey())?bytesToHex(N.getPublicKey().toByteArray()):"") +"%"+
				"";
			if(r.hasXpub())
				return NodeStr + ":!:" + r.getXpub() + ":!:" +
					bytesToHex(r.getXpubBytes().toByteArray());
			return NodeStr; }
        case "MessageSignature": {
            TrezorMessage.MessageSignature r = (TrezorMessage.MessageSignature)resp;
            return new String(Hex.encode(r.getSignature().toByteArray())); }
        case "TxRequest": {
            TrezorMessage.TxRequest r = (TrezorMessage.TxRequest)resp;
            TrezorType.TransactionType ackTx;
            if (r.getSerialized() != null && r.getSerialized().hasSignatureIndex()) {
                curSignatures.set(r.getSerialized().getSignatureIndex(),
                        Hex.toHexString(r.getSerialized().getSignature().toByteArray()));
            }
            if (r.getRequestType().equals(TrezorType.RequestType.TXFINISHED)) {
                return Joiner.on(";").join(curSignatures);
            } else if (r.getRequestType().equals(TrezorType.RequestType.TXINPUT)) {
                ackTx = TrezorType.TransactionType.newBuilder().
                    clearInputs().
                    addInputs(createInput(r.getDetails().hasTxHash() ? r.getDetails().getTxHash() : null,
                            r.getDetails().getRequestIndex())).
                    build();
            } else if (r.getRequestType().equals(TrezorType.RequestType.TXOUTPUT)) {
                if (r.getDetails().hasTxHash()) {
                    ackTx = TrezorType.TransactionType.newBuilder().
                            clearOutputs().
                            addBinOutputs(createBinOutput(
                                    r.getDetails().getTxHash(),
                                    r.getDetails().getRequestIndex())).
                            build();
                } else {
                    ackTx = TrezorType.TransactionType.newBuilder().
                        clearOutputs().
                        addOutputs(createOutput(r.getDetails().getRequestIndex())).
                        build();
                }
            } else if (r.getRequestType().equals(TrezorType.RequestType.TXMETA)) {
                TrezorType.TransactionType.Builder b = TrezorType.TransactionType.newBuilder();
                if (r.getDetails().hasTxHash()) {
                    final Transaction tx = curTx.prevoutRawTxs.get(Hex.toHexString(r.getDetails().getTxHash().toByteArray()));
                    b.setInputsCnt(tx.getInputs().size());
                    b.setOutputsCnt(tx.getOutputs().size());
					b.setVersion((int) tx.getVersion())
							.setLockTime((int) tx.getLockTime());
                } else {
                    b.setInputsCnt(curTx.decoded.getInputs().size());
                    b.setOutputsCnt(curTx.decoded.getOutputs().size());
					b.setVersion((int) curTx.decoded.getVersion())
							.setLockTime((int) curTx.decoded.getLockTime());
                }
                ackTx = b.build();
            } else {
                return resp.getClass().getSimpleName();
            }
            return _get(this.send(TrezorMessage.TxAck.newBuilder().
                setTx(ackTx).build())); }
		}
//		throw new IllegalArgumentException();
		return resp.getClass().getSimpleName();
	}

    private TrezorType.TxOutputType createOutput(int requestIndex) {
        TransactionOutput txOut = curTx.decoded.getOutputs().get(requestIndex);

        TrezorType.TxOutputType.Builder b = TrezorType.TxOutputType.newBuilder().
            setAmount(txOut.getValue().longValue());
        if (txOut.getScriptPubKey().isPayToScriptHash()) {
            b.setAddress(txOut.getAddressFromP2SH(Network.NETWORK).toString());
            if (txOut.getAddressFromP2SH(Network.NETWORK).equals(curChangeAddr)) {
                b.setScriptType(TrezorType.OutputScriptType.PAYTOMULTISIG);
                if (curRecoveryNode == null) {
                    b.setMultisig(TrezorType.MultisigRedeemScriptType.newBuilder().
                            clearPubkeys().
                            addPubkeys(TrezorType.HDNodePathType.newBuilder().
                                    setNode(curGaNode).
                                    clearAddressN().
                                    addAddressN(Integer.valueOf(curTx.change_pointer).intValue())).
                            addPubkeys(TrezorType.HDNodePathType.newBuilder().
                                    setNode(curWalletNode).
                                    clearAddressN().
                                    addAddressN(Integer.valueOf(curTx.change_pointer).intValue())).
                            setM(2));
                } else {
                    b.setMultisig(TrezorType.MultisigRedeemScriptType.newBuilder().
                            clearPubkeys().
                            addPubkeys(TrezorType.HDNodePathType.newBuilder().
                                    setNode(curGaNode).
                                    clearAddressN().
                                    addAddressN(Integer.valueOf(curTx.change_pointer).intValue())).
                            addPubkeys(TrezorType.HDNodePathType.newBuilder().
                                    setNode(curWalletNode).
                                    clearAddressN().
                                    addAddressN(Integer.valueOf(curTx.change_pointer).intValue())).
                            addPubkeys(TrezorType.HDNodePathType.newBuilder().
                                    setNode(curRecoveryNode).
                                    clearAddressN().
                                    addAddressN(Integer.valueOf(curTx.change_pointer).intValue())).
                            setM(2));
                }
            } else {
                b.setScriptType(TrezorType.OutputScriptType.PAYTOSCRIPTHASH);
            }
        } else {
            b.setAddress(txOut.getAddressFromP2PKHScript(Network.NETWORK).toString());
            b.setScriptType(TrezorType.OutputScriptType.PAYTOADDRESS);
        }

        return b.build();
    }

    private TrezorType.TxOutputBinType createBinOutput(ByteString txHash, int requestIndex) {
        final TransactionOutput out = curTx.prevoutRawTxs.get(Hex.toHexString(txHash.toByteArray())).getOutput(requestIndex);
        return TrezorType.TxOutputBinType.newBuilder().
            setAmount(out.getValue().longValue()).
            setScriptPubkey(ByteString.copyFrom(out.getScriptBytes())).
            build();
    }

    private TrezorType.TxInputType createInput(ByteString txHash, int requestIndex) {
        final TransactionInput in;
        if (txHash != null) {
            in = curTx.prevoutRawTxs.get(Hex.toHexString(txHash.toByteArray())).getInput(requestIndex);
            return TrezorType.TxInputType.newBuilder().
                    setPrevHash(ByteString.copyFrom(in.getOutpoint().getHash().getBytes())).
                    setPrevIndex((int)in.getOutpoint().getIndex()).
                    setSequence((int)in.getSequenceNumber()).
                    setScriptSig(ByteString.copyFrom(in.getScriptBytes())).
                    build();
        } else {
            in = curTx.decoded.getInput(requestIndex);
            final Integer[] addrN;
            if (curSubaccount != 0) {
                addrN = new Integer[] { 3 + 0x80000000, curSubaccount + 0x80000000, 1, curTx.prev_outputs.get(requestIndex).getPointer() };
            } else {
                addrN = new Integer[] { 1, curTx.prev_outputs.get(requestIndex).getPointer() };
            }
            final TrezorType.MultisigRedeemScriptType multisig;
            if (curRecoveryNode == null) {
                multisig = TrezorType.MultisigRedeemScriptType.newBuilder().
                        clearPubkeys().
                        addPubkeys(TrezorType.HDNodePathType.newBuilder().
                                setNode(curGaNode).
                                clearAddressN().
                                addAddressN(curTx.prev_outputs.get(requestIndex).getPointer())).
                        addPubkeys(TrezorType.HDNodePathType.newBuilder().
                                setNode(curWalletNode).
                                clearAddressN().
                                addAddressN(curTx.prev_outputs.get(requestIndex).getPointer())).
                        setM(2).
                        build();
            } else {
                multisig = TrezorType.MultisigRedeemScriptType.newBuilder().
                        clearPubkeys().
                        addPubkeys(TrezorType.HDNodePathType.newBuilder().
                                setNode(curGaNode).
                                clearAddressN().
                                addAddressN(curTx.prev_outputs.get(requestIndex).getPointer())).
                        addPubkeys(TrezorType.HDNodePathType.newBuilder().
                                setNode(curWalletNode).
                                clearAddressN().
                                addAddressN(curTx.prev_outputs.get(requestIndex).getPointer())).
                        addPubkeys((TrezorType.HDNodePathType.newBuilder().
                                setNode(curRecoveryNode).
                                clearAddressN().
                                addAddressN(curTx.prev_outputs.get(requestIndex).getPointer()))).
                        setM(2).
                        build();
            }
            return TrezorType.TxInputType.newBuilder().
                    clearAddressN().
                    addAllAddressN(Arrays.asList(addrN)).
                    setPrevHash(ByteString.copyFrom(in.getOutpoint().getHash().getBytes())).
                    setPrevIndex((int)in.getOutpoint().getIndex()).
                    setSequence((int) in.getSequenceNumber()).
                    setScriptType(TrezorType.InputScriptType.SPENDMULTISIG).
                    setMultisig(multisig).
                    build();
        }
    }

    public String MessagePing() {
		return _get(this.send(TrezorMessage.Ping.newBuilder().build()));
	}

	public String MessagePing(String msg) {
		return _get(this.send(
			TrezorMessage.Ping.newBuilder().
			setMessage(msg).
			build()));
	}

	public String MessagePing(String msg, Boolean ButtonProtection) {
		return _get(this.send(
			TrezorMessage.Ping.newBuilder().
			setMessage(msg).
			setButtonProtection(ButtonProtection).
			build()));
	}

	public String MessageGetPublicKey(Integer[] addrn) {
		return _get(this.send(
			TrezorMessage.GetPublicKey.newBuilder().
			clearAddressN().
			addAllAddressN(Arrays.asList(addrn)).
			build()));
	}

    public ECKey.ECDSASignature MessageSignMessage(Integer[] addrn, String message) {
        byte[] sigCompact = Hex.decode(_get(this.send(
                TrezorMessage.SignMessage.newBuilder().
                        clearAddressN().
                        addAllAddressN(Arrays.asList(addrn)).
                        setMessage(ByteString.copyFromUtf8(message)).
                        build())));
        return new ECKey.ECDSASignature(
                new BigInteger(1, Arrays.copyOfRange(sigCompact, 1, 33)),
                new BigInteger(1, Arrays.copyOfRange(sigCompact, 33, 65)));
    }

    public List<Integer> getFirmwareVersion() {
        Iterable<String> version = Splitter.on(".").split(_get(this.send(
                TrezorMessage.Initialize.newBuilder().build()
        )));
        LinkedList<Integer> versionInts = new LinkedList<>();
        for (String s : version) {
            versionInts.add(Integer.valueOf(s));
        }
        return versionInts;
    }

    public List<ECKey.ECDSASignature> MessageSignTx(PreparedTransaction tx, String coinName, final byte[] gait_path) {
        curTx = tx;
        curSubaccount = tx.subaccount_pointer;
        LinkedList<ECKey> keys = new LinkedList<>();
        DeterministicKey gaWallet = new DeterministicKey(
                new ImmutableList.Builder<ChildNumber>().build(),
                Hex.decode(Network.depositChainCode),
                ECKey.fromPublicOnly(Hex.decode(Network.depositPubkey)).getPubKeyPoint(),
                null, null);

        if (tx.subaccount_pointer != 0) {
            gaWallet = HDKeyDerivation.deriveChildKey(gaWallet, new ChildNumber(3, false));
        } else {
            gaWallet = HDKeyDerivation.deriveChildKey(gaWallet, new ChildNumber(1, false));
        }
        int childNum = 0;
        for (int i = 0; i < 32; ++i) {
            int b1 = gait_path[i * 2];
            if (b1 < 0) {
                b1 = 256 + b1;
            }
            int b2 = gait_path[i * 2 + 1];
            if (b2 < 0) {
                b2 = 256 + b2;
            }
            childNum = b1 * 256 + b2;
            gaWallet = HDKeyDerivation.deriveChildKey(gaWallet, new ChildNumber(childNum, false));
        }
        if (tx.subaccount_pointer != 0 ) {
            gaWallet = HDKeyDerivation.deriveChildKey(gaWallet, new ChildNumber(tx.subaccount_pointer, false));
        }
        curGaNode = TrezorType.HDNodeType.newBuilder().
            setDepth(tx.subaccount_pointer == 0 ? 33 : 34).
            setFingerprint(0).
            setChildNum(tx.subaccount_pointer == 0 ? childNum : tx.subaccount_pointer).
            setPublicKey(ByteString.copyFrom(gaWallet.getPubKey())).
            setChainCode(ByteString.copyFrom(gaWallet.getChainCode())).
            build();
		final Script changeScript;
		if (tx.change_pointer != null) {
			DeterministicKey changeKey = HDKeyDerivation.deriveChildKey(gaWallet, new ChildNumber(Integer.valueOf(tx.change_pointer).intValue()));
			keys.add(ECKey.fromPublicOnly(changeKey.getPubKeyPoint()));

			final Integer[] intArray;
			if (tx.subaccount_pointer != 0) {
				intArray = new Integer[]{3 + 0x80000000, tx.subaccount_pointer + 0x80000000, 1, Integer.valueOf(tx.change_pointer)};
			} else {
				intArray = new Integer[]{1, Integer.valueOf(tx.change_pointer)};
			}
			final String[] xpub = MessageGetPublicKey(intArray).split("%", -1);
			final String pkHex = xpub[xpub.length - 2];
			keys.add(ECKey.fromPublicOnly(Hex.decode(pkHex)));

			if (tx.twoOfThreeBackupChaincode != null) {
				DeterministicKey backupWallet = new DeterministicKey(
						new ImmutableList.Builder<ChildNumber>().build(),
						Hex.decode(tx.twoOfThreeBackupChaincode),
						ECKey.fromPublicOnly(Hex.decode(tx.twoOfThreeBackupPubkey)).getPubKeyPoint(),
						null, null);
				backupWallet = HDKeyDerivation.deriveChildKey(backupWallet, new ChildNumber(1, false));

				curRecoveryNode = TrezorType.HDNodeType.newBuilder().
						setDepth(1).
						setFingerprint(0).
						setChildNum(1).
						setPublicKey(ByteString.copyFrom(backupWallet.getPubKey())).
						setChainCode(ByteString.copyFrom(backupWallet.getChainCode())).
						build();

				backupWallet = HDKeyDerivation.deriveChildKey(backupWallet, new ChildNumber(Integer.valueOf(tx.change_pointer)));
				keys.add(ECKey.fromPublicOnly(backupWallet.getPubKeyPoint()));
			} else {
				curRecoveryNode = null;
			}

			changeScript = new Script(Script.createMultiSigOutputScript(2, keys));
		} else {
			changeScript = null;
		}
		if (changeScript != null) {
			try {
				curChangeAddr = new org.bitcoinj.core.Address(Network.NETWORK,
						Network.NETWORK.getP2SHHeader(),
						Utils.sha256hash160(changeScript.getProgram()));
			} catch (WrongNetworkException e) {
				curChangeAddr = null;
			}
		} else {
			curChangeAddr = null;
		}


        final Integer[] intArray2;
        if (tx.subaccount_pointer != 0) {
            intArray2 = new Integer[]{3 + 0x80000000, tx.subaccount_pointer + 0x80000000, 1};
        } else {
            intArray2 = new Integer[]{1};
        }
        final String[] xpub2 = MessageGetPublicKey(intArray2).split("%", -1);
        final String pkHex2 = xpub2[xpub2.length-2];
        final String chainCodeHex2 = xpub2[xpub2.length-4];

        curWalletNode = TrezorType.HDNodeType.newBuilder().
                setDepth(1).
                setFingerprint(0).
                setChildNum(1).
                setPublicKey(ByteString.copyFrom(Hex.decode(pkHex2))).
                setChainCode(ByteString.copyFrom(Hex.decode(chainCodeHex2))).
                build();

        curSignatures.clear();
        for (int i = 0; i < curTx.decoded.getInputs().size(); ++i) {
            curSignatures.add("");
        }

        LinkedList<ECKey.ECDSASignature> signaturesList = new LinkedList<>();
        String[] signatures = _get(this.send(
                SignTx.newBuilder().
                        setInputsCount(tx.decoded.getInputs().size()).
                        setOutputsCount(tx.decoded.getOutputs().size()).
                        setCoinName(coinName).
                        build())).split(";");
        for (int i = 0; i < signatures.length; ++i) {
            signaturesList.add(ECKey.ECDSASignature.decodeFromDER(Hex.decode(signatures[i])));
        }
        return signaturesList;
    }
}
