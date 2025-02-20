package com.greenaddress.greenbits.wallets;

import static io.ktor.util.CryptoKt.hex;

import android.app.Activity;
import android.content.Context;
import android.nfc.NfcAdapter;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.blockstream.common.devices.DeviceBrand;
import com.blockstream.common.devices.DeviceModel;
import com.blockstream.common.gdk.data.Account;
import com.blockstream.common.gdk.data.Device;
import com.blockstream.common.gdk.data.InputOutput;
import com.blockstream.common.gdk.data.Network;
import com.blockstream.common.gdk.device.BlindingFactorsResult;
import com.blockstream.common.gdk.device.GdkHardwareWallet;
import com.blockstream.common.gdk.device.HardwareWalletInteraction;
import com.blockstream.common.gdk.device.SignMessageResult;
import com.blockstream.common.gdk.device.SignTransactionResult;
import com.blockstream.libwally.Wally;
import com.btchip.utils.BufferUtils;
import com.btchip.utils.VarintUtils;
import com.google.common.base.Joiner;
import com.satochip.ApduException;
import com.satochip.ApduResponse;
import com.satochip.ApplicationStatus;
import com.satochip.Bip32Path;
import com.satochip.BlockedPINException;
import com.satochip.CardChannel;
import com.satochip.CardListener;
import com.satochip.NfcActionObject;
import com.satochip.NfcActionStatus;
import com.satochip.NfcActionType;
import com.satochip.NfcCardManager;
import com.satochip.SatochipCommandSet;
import com.satochip.SatochipException;
import com.satochip.SatochipParser;
import com.satochip.WrongPINException;
import com.satochip.WrongPINLegacyException;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kotlinx.coroutines.flow.MutableStateFlow;


public class SatochipHWWallet extends GdkHardwareWallet implements CardListener {

    private static final String TAG = SatochipHWWallet.class.getSimpleName();

    /** The string that prefixes all text messages signed using Bitcoin keys. */
    private static final String BITCOIN_SIGNED_MESSAGE_HEADER = "Bitcoin Signed Message:\n";
    private static final byte[] BITCOIN_SIGNED_MESSAGE_HEADER_BYTES = BITCOIN_SIGNED_MESSAGE_HEADER.getBytes(StandardCharsets.UTF_8);

    private static final byte SIGHASH_ALL = 1;

    private final Map<String, String> mUserXPubs = new HashMap<>();

    private final Device device;
    private final DeviceModel model;
    private final String firmwareVersion = "x.y-z.w"; //todo

    private String pin;
    private final Activity activity;
    private final Context context;
    private final CardChannel channel = null;
    //private final SatochipCommandSet cmdSet;
    private NfcAdapter nfcAdapter = null;

    //private NfcActionType actionType = NfcActionType.none;
    //private NfcActionResult actionResult = null;
    private NfcActionObject actionObject = new NfcActionObject();


    public SatochipHWWallet(Device device, String pin,  Activity activity, Context context){
        Log.i(TAG, "constructor start");
        //mTrezor = t;
        this.device = device;
        //String model = "Satochip";
        this.model = DeviceModel.SatochipGeneric;
        this.pin = pin;
        this.activity = activity;
        this.context = context;

        NfcCardManager cardManager = new NfcCardManager();
        cardManager.setCardListener(this);
        cardManager.start();
        Log.i(TAG, "SATODEBUG SatochipHWWallet getXpubs() after cardManager start");

        nfcAdapter = NfcAdapter.getDefaultAdapter(this.context);
        nfcAdapter.enableReaderMode(
                activity,
                cardManager,
                NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_NFC_B | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                null
        );
        Log.i(TAG, "SATODEBUG SatochipHWWallet getXpubs() after NfcAdapter.enableReaderMode");

    }

    @Override
    public synchronized void disconnect() {
        Log.i(TAG, "disconnect start");
        // No-op
    }


    public void onConnected(CardChannel channel) {

        Log.i(TAG, "SATODEBUG SatochipHWWallet onConnected() Card is connected");
        try {

            if (this.actionObject.actionType == NfcActionType.none) {
                Log.i(TAG, "SATODEBUG SatochipHWWallet onConnected() nothing to do => disconnection!");
                onDisconnected();
                return;
            }

            SatochipCommandSet cmdSet = new SatochipCommandSet(channel);
            Log.i(TAG, "SATODEBUG SatochipHWWallet onConnected() cmdSet created");
            // start to interact with card
            //NfcCardService.initialize(cmdSet)

            ApduResponse rapduSelect = cmdSet.cardSelect("satochip").checkOK();
            Log.i(TAG, "SATODEBUG SatochipHWWallet onConnected() applet selected");
            // cardStatus
            ApduResponse rapduStatus = cmdSet.cardGetStatus();//To update status if it's not the first reading
            ApplicationStatus cardStatus = cmdSet.getApplicationStatus(); //applicationStatus ?: return
            Log.i(TAG, "SATODEBUG SatochipHWWallet readCard cardStatus: $cardStatus");
            Log.i(TAG, "SATODEBUG SatochipHWWallet readCard cardStatus: ${cardStatus.toString()}");


            // verify PIN
            if (this.pin != null) {
                //String pinStr = "123456"; //todo
                byte[] pinBytes = this.pin.getBytes(Charset.forName("UTF-8"));
                ApduResponse rapduPin = cmdSet.cardVerifyPIN(pinBytes);
                Log.i(TAG, "SATODEBUG SatochipHWWallet onConnected() pin verified: " + rapduPin.getSw());
                Log.i(TAG, "SATODEBUG SatochipHWWallet onConnected() pin verified: " + rapduPin.toHexString());
                // todo: wrong pin/blockecd pin...
            } else {
                throw new SatochipException(SatochipException.ExceptionReason.PIN_UNDEFINED);
            }

            // execute commands depending on actionType
            if (this.actionObject.actionType == NfcActionType.getXpubs){
                onConnectedGetXpubs(cmdSet);
            } else if (this.actionObject.actionType == NfcActionType.signMessage) {
                onConnectedSignMessage(cmdSet);
            } else if (this.actionObject.actionType == NfcActionType.signTransaction){
                onConnectedSignTransaction(cmdSet);
            }

            // TODO: disconnect?
            Log.i(TAG, "SATODEBUG SatochipHWWallet onConnected() trigger disconnection!");
            onDisconnected();

        } catch (SatochipException e) {
            this.pin = null;
            Log.e(TAG, "SATODEBUG onConnected: ERROR: "+ e);
            onDisconnected();
            throw new RuntimeException(e); // todo: msg user instead!!
        } catch (WrongPINException e) {
            this.pin = null;
            Log.e(TAG, "SATODEBUG onConnected: WRONG PIN! "+ e);
            onDisconnected();
            throw new RuntimeException(e); // todo: msg user instead!!
        } catch (WrongPINLegacyException e) {
            this.pin = null;
            Log.e(TAG, "SATODEBUG onConnected: WRONG PIN LEGACY! "+ e);
            onDisconnected();
            throw new RuntimeException(e); // todo: msg user instead!!
        } catch (BlockedPINException e) {
            this.pin = null;
            Log.e(TAG, "SATODEBUG onConnected: A an exception has been thrown during card init: "+ e);
            onDisconnected();
            throw new RuntimeException(e); // todo: msg user instead!!
        } catch (ApduException e) {
            //throw new RuntimeException(e);
            Log.e(TAG, "SATODEBUG onConnected: A an exception has been thrown during card init: "+ e);
            e.printStackTrace();
            onDisconnected();
        } catch (IOException e) {
            //throw new RuntimeException(e);
            Log.e(TAG, "SATODEBUG onConnected: B an exception has been thrown during card init: " + e);
            e.printStackTrace();
            onDisconnected();
        } catch (Exception e) {
            Log.e(TAG, "SATODEBUG onConnected: C an exception has been thrown during card init.: " + e);
            e.printStackTrace();
            onDisconnected();
        }
    }

    public void onConnectedGetXpubs(SatochipCommandSet cmdSet) throws Exception {

        // get paths
        List<? extends List<Integer>> paths = this.actionObject.pathsParam;
        final List<String> xpubs = new ArrayList<>(paths.size());

        for (List<Integer> path : paths) {
            Log.i(TAG, "SATODEBUG SatochipHWWallet onConnectedGetXpubs() path: " + path);
            final String key = Joiner.on("/").join(path);
//            Log.i(TAG, "SATODEBUG SatochipHWWallet onConnectedGetXpubs() key: " + key + ".");

            if (!mUserXPubs.containsKey(key)) {

                Bip32Path bip32path = pathToBip32Path(path);
                Log.i(TAG, "SATODEBUG SatochipHWWallet onConnectedGetXpubs() depth: " + bip32path.getDepth()); //debug
                Log.i(TAG, "SATODEBUG SatochipHWWallet onConnectedGetXpubs() bip32path: " + hex(bip32path.getBytes())); //debug

                // get xpub from satochip
                //pathStr = "m/44'/0'/0'/0";
                int xtype = this.actionObject.networkParam.getVerPublic(); //0x0488B21E;
                Integer sid = null;
                String xpub = cmdSet.cardBip32GetXpub(bip32path, xtype, sid);
                Log.i(TAG, "SATODEBUG SatochipHWWallet onConnectedGetXpubs() xpub: " + xpub);

                // cache xpub
                mUserXPubs.put(key, xpub);

            }{
                Log.i(TAG, "SATODEBUG SatochipHWWallet onConnectedGetXpubs() recovered xpub from CACHE");
            }
            // update xpubs list
            xpubs.add(mUserXPubs.get(key));
        }

        this.actionObject.xpubsResult = xpubs;

        // action finished
        this.actionObject.actionStatus = NfcActionStatus.finished;
    }

    public void onConnectedSignMessage(SatochipCommandSet cmdSet) throws Exception {

        // get path
        List<Integer> path = this.actionObject.pathParam;
        Log.i(TAG, "SATODEBUG SatochipHWWallet onConnectedSignMessage() path: " + path);

        // derive key DEBUG
//        String pathStr = "m/84'/0'/0'/0/0";
//        Log.i(TAG, "SATODEBUG SatochipHWWallet onConnectedSignMessage() pathStr: " + pathStr); //debug
//        byte optionFlags = (byte) 0x40;
//        byte[][] extendedKey = cmdSet.cardBip32GetExtendedKey(pathStr, optionFlags, null);

        // derive key
        Bip32Path bip32path = pathToBip32Path(path);
        Log.i(TAG, "SATODEBUG SatochipHWWallet onConnectedSignMessage() depth: " + bip32path.getDepth()); //debug
        Log.i(TAG, "SATODEBUG SatochipHWWallet onConnectedSignMessage() bip32path: " + hex(bip32path.getBytes())); //debug
        byte optionFlags = (byte) 0x40;
        byte[][] extendedKey = cmdSet.cardBip32GetExtendedKey(bip32path, optionFlags, null);
        Log.i(TAG, "SATODEBUG SatochipHWWallet onConnectedSignMessage() extendedKey_0: " + hex(extendedKey[0])); //debug
        Log.i(TAG, "SATODEBUG SatochipHWWallet onConnectedSignMessage() extendedKey_1: " + hex(extendedKey[1])); //debug

        // compute message hash
        String message = this.actionObject.messageParam;
        //String message = "DEBUG TEST SATOCHIP";
        Log.i(TAG, "SATODEBUG SatochipHWWallet onConnectedSignMessage() message: " + message); //debug
        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        Log.i(TAG, "SATODEBUG SatochipHWWallet onConnectedSignMessage() messageBytes: " + hex(messageBytes)); //debug
        byte[] formatedMessageBytes;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bos.write(BITCOIN_SIGNED_MESSAGE_HEADER_BYTES.length);
            bos.write(BITCOIN_SIGNED_MESSAGE_HEADER_BYTES);
//            VarInt size = new VarInt(messageBytes.length);
//            bos.write(size.encode());
            VarintUtils.write(bos, messageBytes.length);
            bos.write(messageBytes);
            formatedMessageBytes = bos.toByteArray();
            Log.i(TAG, "SATODEBUG SatochipHWWallet onConnectedSignMessage() formatedMessageBytes: " + hex(formatedMessageBytes)); //debug
        } catch (IOException e) {
            throw new RuntimeException(e);  // Cannot happen.
        }
        byte[] hashBytes = Wally.sha256d(formatedMessageBytes); // double hash
        //byte[] hashBytes = Wally.sha256(formatedMessageBytes); // single hash debug?
        Log.i(TAG, "SATODEBUG SatochipHWWallet onConnectedSignMessage() hashBytes: " + hex(hashBytes)); //debug

        // debug
        byte[] messageBytesWally = Wally.format_bitcoin_message(messageBytes, 0); // should be equal to formatedMessageBytes
        Log.i(TAG, "SATODEBUG SatochipHWWallet onConnectedSignMessage() messageBytesWally: " + hex(messageBytesWally)); //debug
        byte[] messageBytesWally2 = Wally.format_bitcoin_message(messageBytes, 1); // should be equal to hashBytes
        Log.i(TAG, "SATODEBUG SatochipHWWallet onConnectedSignMessage() messageBytesWally2: " + hex(messageBytesWally2)); //debug

        // sign hash
        byte keynbr = (byte) 0xFF;
        byte[] chalresponse = null;
        ApduResponse rapdu = cmdSet.cardSignHash(keynbr, hashBytes, chalresponse);
        byte[] sigBytes = rapdu.getData();
        Log.i(TAG, "SATODEBUG SatochipHWWallet onConnectedSignMessage() sigBytes: " + hex(sigBytes)); //debug

        // verify sig
        SatochipParser parser = new SatochipParser();
        //boolean isOk = parser.verifySig(formatedMessageBytes, sigBytes, extendedKey[0]);
        boolean isOk = parser.verifySig(Wally.sha256(formatedMessageBytes), sigBytes, extendedKey[0]);
        Log.i(TAG, "SATODEBUG SatochipHWWallet onConnectedSignMessage() isOk: " + isOk); //debug

        // format signature
        final String sigHex = Wally.hex_from_bytes(sigBytes);
        Log.i(TAG, "SATODEBUG SatochipHWWallet onConnectedSignMessage() sigHex: " + sigHex); //debug
        this.actionObject.signatureResult = sigHex;

        // action finished
        this.actionObject.actionStatus = NfcActionStatus.finished;
    }

    public void onConnectedSignTransaction(SatochipCommandSet cmdSet) throws Exception {

        // get paths, hashes & set sig result
        final List<? extends List<Integer>> paths = this.actionObject.pathsParam;
        final List<byte[]> hashes = this.actionObject.hashesParam;
        final int inputSize = paths.size();
        List<String> signaturesResult = new ArrayList<>(inputSize);
        for (int i = 0; i < inputSize; ++i) {

            // get path
            List<Integer> path = paths.get(i);
            Log.i(TAG, "SATODEBUG SatochipHWWallet onConnectedSignTransaction() path: " + path);

            // derive key
            Bip32Path bip32path = pathToBip32Path(path);
            Log.i(TAG, "SATODEBUG SatochipHWWallet onConnectedSignTransaction() depth: " + bip32path.getDepth()); //debug
            Log.i(TAG, "SATODEBUG SatochipHWWallet onConnectedSignTransaction() bip32path: " + hex(bip32path.getBytes())); //debug
            byte optionFlags = (byte) 0x40;
            byte[][] extendedKey = cmdSet.cardBip32GetExtendedKey(bip32path, optionFlags, null);
            Log.i(TAG, "SATODEBUG SatochipHWWallet onConnectedSignTransaction() extendedKey_0: " + hex(extendedKey[0])); //debug
            Log.i(TAG, "SATODEBUG SatochipHWWallet onConnectedSignTransaction() extendedKey_1: " + hex(extendedKey[1])); //debug

            // compress card
            byte[] compressedPubkey = Arrays.copyOf(extendedKey[0], Wally.EC_PUBLIC_KEY_LEN);
            if (compressedPubkey[Wally.EC_PUBLIC_KEY_LEN-1]%2 == 0){
                compressedPubkey[0] = 0x02;
            } else {
                compressedPubkey[0] = 0x03;
            }

            // get & sign hash
            byte[] hashBytes = hashes.get(i);
            byte keynbr = (byte) 0xFF;
            byte[] chalresponse = null;
            ApduResponse rapdu = cmdSet.cardSignHash(keynbr, hashBytes, chalresponse);
            byte[] sigBytes = rapdu.getData();
            Log.i(TAG, "SATODEBUG SatochipHWWallet onConnectedSignTransaction() sigBytes: " + hex(sigBytes)); //debug

            // verify sig
//            SatochipParser parser = new SatochipParser();
//            boolean isOk = parser.verifySig(hashBytes, sigBytes, extendedKey[0]);
//            Log.i(TAG, "SATODEBUG SatochipHWWallet onConnectedSignTransaction() isOk: " + isOk); //debug

            // convert to compact
            byte[] compactSigBytes = Wally.ec_sig_from_der(sigBytes);
            Log.i(TAG, "SATODEBUG SatochipHWWallet onConnectedSignTransaction() compactSigBytes: " + hex(compactSigBytes)); //debug

            // verify sig
//            int compactSigVerif = Wally.ec_sig_verify(compressedPubkey, hashBytes, Wally.EC_FLAG_ECDSA, compactSigBytes);
//            Log.i(TAG, "SATODEBUG SatochipHWWallet onConnectedSignTransaction() compactSigVerif: " + compactSigVerif); //debug

            // sanitize sig
            byte[] lowsCompactSigBytes = new byte[Wally.EC_SIGNATURE_LEN];
            Wally.ec_sig_normalize(compactSigBytes, lowsCompactSigBytes);
            Log.i(TAG, "SATODEBUG SatochipHWWallet onConnectedSignTransaction() lowsCompactSigBytes: " + hex(lowsCompactSigBytes)); //debug

            // verify sig
//            int lowsCompactSigVerif = Wally.ec_sig_verify(compressedPubkey, hashBytes, Wally.EC_FLAG_ECDSA, lowsCompactSigBytes);
//            Log.i(TAG, "SATODEBUG SatochipHWWallet onConnectedSignTransaction() lowsCompactSigVerif: " + lowsCompactSigVerif); //debug

            // convert back to der
            final byte[] derSigBytes = new byte[Wally.EC_SIGNATURE_DER_MAX_LEN];
            final int len = Wally.ec_sig_to_der(lowsCompactSigBytes, derSigBytes);
            final String sigHex = Wally.hex_from_bytes(Arrays.copyOf(derSigBytes, len)) + "01";

            Log.i(TAG, "SATODEBUG SatochipHWWallet onConnectedSignTransaction() sigHex: " + sigHex); //debug
            signaturesResult.add(sigHex);
        }

        // action finished
        this.actionObject.signaturesResult = signaturesResult;
        this.actionObject.actionStatus = NfcActionStatus.finished;
    }

    // SATODEBUG
    public void onDisconnected() {
        //NfcCardService.isConnected.postValue(false)
        Log.i(TAG, "SATODEBUG SatochipHWWallet onDisconnected: Card disconnected!");
    }

    @NonNull
    @Override
    public synchronized List<String> getXpubs(@NonNull Network network, @NonNull List<? extends List<Integer>> paths, @Nullable HardwareWalletInteraction hwInteraction) {
        Log.i("SatochipHWWallet", "SATODEBUG SatochipHWWallet getXpubs start");
        Log.i("SatochipHWWallet", "SATODEBUG SatochipHWWallet getXpubs paths: " + paths);
        Log.i("SatochipHWWallet", "SATODEBUG SatochipHWWallet getXpubs HardwareWalletInteraction: " + hwInteraction);


        // first step: check if xpubs are available in cache
        boolean isCached = true;
        final List<String> cachedXpubs = new ArrayList<>(paths.size());
        for (List<Integer> path : paths) {
            Log.i(TAG, "SATODEBUG SatochipHWWallet getXpubs() path: " + path);
            final String key = Joiner.on("/").join(path);
            if (mUserXPubs.containsKey(key)) {
                cachedXpubs.add(mUserXPubs.get(key));
            } else {
                isCached = false;
            }
        }
        if (isCached){
            Log.i(TAG, "SATODEBUG SatochipHWWallet getXpubs() XPUBS IN CACHE!");
            return cachedXpubs;
        }

        // request to card if not cached already
        try {
            this.actionObject.actionStatus = NfcActionStatus.busy;
            this.actionObject.actionType = NfcActionType.getXpubs;
            this.actionObject.networkParam = network;
            this.actionObject.pathsParam = paths;

            // poll for result from cardListener onConnected
            while (this.actionObject.actionStatus == NfcActionStatus.busy) {
                TimeUnit.MILLISECONDS.sleep(500);
                Log.i(TAG, "SATODEBUG SatochipHWWallet getXpubs() SLEEP");
            }

            final List<String> xpubs = this.actionObject.xpubsResult;
            return xpubs;

        } catch (Exception e) {
            Log.i("SatochipHWWallet", "getXpubs exception: " + e);
        }

        return null;
    }

    @NonNull
    @Override
    public SignMessageResult signMessage(@NonNull List<Integer> path, @NonNull String message, boolean useAeProtocol, @Nullable String aeHostCommitment, @Nullable String aeHostEntropy, @Nullable HardwareWalletInteraction hwInteraction) {
        Log.i("SatochipHWWallet", "signMessage start");
        Log.i("SatochipHWWallet", "signMessage start path: " + path);
        Log.i("SatochipHWWallet", "signMessage start message: " + message);
        Log.i("SatochipHWWallet", "signMessage start useAeProtocol: " + useAeProtocol);
        Log.i("SatochipHWWallet", "signMessage start aeHostCommitment: " + aeHostCommitment);
        Log.i("SatochipHWWallet", "signMessage start aeHostEntropy: " + aeHostEntropy);
        Log.i("SatochipHWWallet", "signMessage start hwInteraction: " + hwInteraction);

        if (useAeProtocol) {
            throw new RuntimeException("Hardware Wallet does not support the Anti-Exfil protocol");
        }

        try {
            this.actionObject.actionStatus = NfcActionStatus.busy;
            this.actionObject.actionType = NfcActionType.signMessage;
            this.actionObject.pathParam = path;
            this.actionObject.messageParam = message;

            // poll for result from cardListener onConnected
            while (this.actionObject.actionStatus == NfcActionStatus.busy) {
                TimeUnit.MILLISECONDS.sleep(500);
                Log.i(TAG, "SATODEBUG SatochipHWWallet signMessage() SLEEP");
            }

            final String signatureResult = this.actionObject.signatureResult;
            Log.i(TAG, "SATODEBUG SatochipHWWallet signMessage() signatureResult: " + signatureResult);
            return new SignMessageResult(signatureResult, null);
        } catch (Exception e) {
            Log.e("SatochipHWWallet", "signMessage exception: " + e);
        }

        //TODO
        String signature = "TODO-SATOCHIP-SIGN-MSG";
        return new SignMessageResult(signature, null);
    }

    @NonNull
    @Override
    public SignTransactionResult signTransaction(@NonNull Network network, @NonNull String transaction, @NonNull List<InputOutput> inputs, @NonNull List<InputOutput> outputs, @Nullable Map<String, String> transactions, boolean useAeProtocol, @Nullable HardwareWalletInteraction hwInteraction) {
        Log.i("SatochipHWWallet", "signTransaction start");
        Log.i("SatochipHWWallet", "signTransaction start network: " + network);
        Log.i("SatochipHWWallet", "signTransaction start transaction: " + transaction);
        Log.i("SatochipHWWallet", "signTransaction start inputs: " + inputs);
        Log.i("SatochipHWWallet", "signTransaction start outputs: " + outputs);
        Log.i("SatochipHWWallet", "signTransaction start transactions: " + transactions);

        final byte[] txBytes = Wally.hex_to_bytes(transaction);
        Log.i("SatochipHWWallet", "signTransaction txBytes: " + Wally.hex_from_bytes(txBytes));

        if(network.isLiquid()){
            throw new RuntimeException(network.getCanonicalName() + " is not supported");
        }

        try {

            if (useAeProtocol) {
                throw new RuntimeException("Hardware Wallet does not support the Anti-Exfil protocol");
            }

            final Object wallyTx = Wally.tx_from_bytes(txBytes, Wally.WALLY_TX_FLAG_USE_WITNESS);
            Log.i("SatochipHWWallet", "signTransaction wallyTx: " + wallyTx);

            boolean sw = false;
            boolean p2sh = false;
            for (final InputOutput in : inputs) {
                Log.i("SatochipHWWallet", "signTransaction inputs[i]: " + in);
                if (in.isSegwit()) {
                    sw = true;
                } else {
                    p2sh = true;
                }
            }
            Log.i("SatochipHWWallet", "signTransaction sw: " + sw);
            Log.i("SatochipHWWallet", "signTransaction p2sh: " + p2sh);

            // debug
            for (final InputOutput out : outputs) {
                Log.i("SatochipHWWallet", "signTransaction outputs[i]: " + out);
            }

            // get tx hash signature for each inputs
            final int inputSize = inputs.size();
            final List<List<Integer>> pathsParam = new ArrayList<>(inputSize);
            final List<byte[]> hashesParam = new ArrayList<>(inputSize);
            Log.i("SatochipHWWallet", "signTransaction() inputs.size(): " + inputSize);
            for (int i = 0; i < inputSize; ++i) {
                Log.i("SatochipHWWallet", "signTransaction() input index: " + i);
                final InputOutput in = inputs.get(i);
                Log.i("SatochipHWWallet", "signTransaction() inputs[i]: " + in);
                final byte[] script = Wally.hex_to_bytes(in.getPrevoutScript());
                Log.i("SatochipHWWallet", "signTransaction() input script[i]: " + Wally.hex_from_bytes(script));
                final long satoshi = in.getSatoshi();
                Log.i("SatochipHWWallet", "signTransaction() input satoshi[i]: " + satoshi);
                final long sighash = SIGHASH_ALL;
                final long flags = Wally.WALLY_TX_FLAG_USE_WITNESS;

                byte[] hash_out = new byte[32];
                //tx_get_btc_signature_hash(Object jarg1, long jarg2, byte[] jarg3, long jarg5, long jarg6, long jarg7, byte[] jarg8);
                byte[] hash = Wally.tx_get_btc_signature_hash(
                        wallyTx,
                        i,
                        script,
                        satoshi,
                        sighash,
                        flags,
                        hash_out
                );
                Log.i("SatochipHWWallet", "signTransaction() input hash_out[i]: " + Wally.hex_from_bytes(hash_out));
                //Log.i("SatochipHWWallet", "signTransaction() input hash[i]: " + hash.getClass().getName());
                hashesParam.add(hash_out);

                // derive key for path
                List<Integer> path = in.getUserPathAsInts();
                Log.i("SatochipHWWallet", "signTransaction() input path[i]: " + path);
                pathsParam.add(in.getUserPathAsInts());

            }

            // create action
            this.actionObject.actionStatus = NfcActionStatus.busy;
            this.actionObject.actionType = NfcActionType.signTransaction;
            this.actionObject.pathsParam = pathsParam;
            this.actionObject.hashesParam = hashesParam;

            // poll for result from cardListener onConnected
            while (this.actionObject.actionStatus == NfcActionStatus.busy) {
                TimeUnit.MILLISECONDS.sleep(500);
                Log.i(TAG, "SATODEBUG SatochipHWWallet signTransaction() SLEEP");
            }

            List<String> sigs = this.actionObject.signaturesResult;
            Log.i(TAG, "SATODEBUG SatochipHWWallet signTransaction() signatureResult: " + sigs);
            return new SignTransactionResult(sigs, null);

        } catch (final Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Signing Error: " + e.getMessage());
        }
    }


    @NonNull
    @Override
    public synchronized String getMasterBlindingKey(@Nullable HardwareWalletInteraction hwInteraction) {
        throw new RuntimeException("Master Blinding Key is not supported");
    }

    @Override
    public synchronized String getBlindingKey(String scriptHex, @Nullable HardwareWalletInteraction hwInteraction) {
        throw new RuntimeException("Master Blinding Key is not supported");
    }

    @Override
    public synchronized String getBlindingNonce(String pubkey, String scriptHex, @Nullable HardwareWalletInteraction hwInteraction) {
        throw new RuntimeException("Master Blinding Key is not supported");
    }

    @Override
    public synchronized BlindingFactorsResult getBlindingFactors(final List<InputOutput> inputs, final List<InputOutput> outputs, @Nullable HardwareWalletInteraction hwInteraction) {
        throw new RuntimeException("Master Blinding Key is not supported");
    }

    @Override
    public synchronized String getGreenAddress(final Network network, final Account account, final List<Long> path, final long csvBlocks, HardwareWalletInteraction hwInteraction) {
        Log.i("SatochipHWWallet", "getGreenAddress start");
        if (network.isMultisig()) {
            throw new RuntimeException("Hardware Wallet does not support displaying Green Multisig Shield addresses");
        }

        if (network.isLiquid()) {
            throw new RuntimeException("Hardware Wallet does not support displaying Liquid addresses");
        }

        // todo
        return "TODO";
    }

    @Nullable
    @Override
    public MutableStateFlow getDisconnectEvent() {
        Log.i("SatochipHWWallet", "getDisconnectEvent start");
        return null;
    }

    @Nullable
    @Override
    public String getFirmwareVersion() {
        Log.i("SatochipHWWallet", "getFirmwareVersion start");
        return firmwareVersion;
    }

    @NonNull
    @Override
    public DeviceModel getModel() {
        Log.i("SatochipHWWallet", "getModel start");
        return model;
    }

    @NonNull
    @Override
    public Device getDevice() {
        Log.i("SatochipHWWallet", "getDevice start");
        return device;
    }


    /* */
    private Bip32Path pathToBip32Path(final List<Integer> path) throws Exception {
        final int depth = path.size();
        if (depth == 0) {
            Bip32Path bip32Path = new Bip32Path(0, new byte[0]);
        }
        if (depth > 10) {
            throw new Exception("Path too long");
        }
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        for (final Integer element : path) {
            BufferUtils.writeUint32BE(result, element);
        }
        Bip32Path bip32Path = new Bip32Path(depth, result.toByteArray());
        return bip32Path;
    }


}
