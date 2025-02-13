package com.greenaddress.greenbits.wallets;

import static io.ktor.util.CryptoKt.hex;

import android.app.Activity;
import android.content.Context;
import android.nfc.NfcAdapter;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.TimeUnit;

//import com.blockstream.common.devices.ApduException;
//import com.blockstream.common.devices.ApduResponse;
//import com.blockstream.common.devices.ApplicationStatus;
//import com.blockstream.common.devices.CardChannel;
//import com.blockstream.common.devices.CardListener;
//import com.blockstream.common.devices.DeviceModel;
//import com.blockstream.common.devices.NfcCardManager;
//import com.blockstream.common.devices.SatochipCommandSet; //todo switch to
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
import com.btchip.BTChipException;
import com.btchip.utils.BufferUtils;
import com.google.common.base.Joiner;
import com.satochip.ApduException;
import com.satochip.ApduResponse;
import com.satochip.ApplicationStatus;
import com.satochip.Bip32Path;
import com.satochip.CardChannel;
import com.satochip.CardListener;
import com.satochip.NfcActionObject;
import com.satochip.NfcActionResult;
import com.satochip.NfcActionStatus;
import com.satochip.NfcActionType;
import com.satochip.NfcCardManager;
import com.satochip.SatochipCommandSet;
import com.satoshilabs.trezor.protobuf.TrezorType;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kotlinx.coroutines.flow.MutableStateFlow;


public class SatochipHWWallet extends GdkHardwareWallet implements CardListener {

    private static final String TAG = SatochipHWWallet.class.getSimpleName();

    private final Map<String, String> mUserXPubs = new HashMap<>();
//    private final Map<String, TrezorType.HDNodeType> mServiceXPubs = new HashMap<>();
//    private final Map<String, TrezorType.HDNodeType> mRecoveryXPubs = new HashMap<>();
    private final Map<String, Object> mPrevTxs = new HashMap<>();

    private final Device device;
    private final DeviceModel model;
    private final String firmwareVersion = "x.y-z.w"; //todo

    private final Activity activity;
    private final Context context;
    private final CardChannel channel = null;
    //private final SatochipCommandSet cmdSet;
    private NfcAdapter nfcAdapter = null;

    //private NfcActionType actionType = NfcActionType.none;
    //private NfcActionResult actionResult = null;
    private NfcActionObject actionObject = new NfcActionObject();


    public SatochipHWWallet(Device device, Activity activity, Context context){
        Log.i(TAG, "constructor start");
        //mTrezor = t;
        this.device = device;
        //String model = "Satochip";
        this.model = DeviceModel.SatochipGeneric;

        this.activity = activity;
        this.context = context;

        //this.cmdSet = new SatochipCommandSet(channel);

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
            String pinStr = "123456"; //todo
            byte[] pinBytes = pinStr.getBytes(Charset.forName("UTF-8"));
            ApduResponse rapduPin = cmdSet.cardVerifyPIN(pinBytes);
            Log.i(TAG, "SATODEBUG SatochipHWWallet onConnected() pin verified: " + rapduPin.getSw());
            Log.i(TAG, "SATODEBUG SatochipHWWallet onConnected() pin verified: " + rapduPin.toHexString());

            // execute commands depending on actionType
            if (this.actionObject.actionType == NfcActionType.getXpub){
                onConnectedGetXpubs(cmdSet);
            }


            // TODO: disconnect?
            Log.i(TAG, "SATODEBUG SatochipHWWallet onConnected() trigger disconnection!");
            onDisconnected();

            // stop polling? check if actionStatus is not busy, or finished?
            //nfcAdapter.disableReaderMode(activity);


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

        // debug
        try {
            this.actionObject.actionStatus = NfcActionStatus.busy;
            this.actionObject.actionType = NfcActionType.getXpub;
            this.actionObject.networkParam = network;
            this.actionObject.pathsParam = paths;

//            NfcCardManager cardManager = new NfcCardManager();
//            cardManager.setCardListener(this);
//            cardManager.start();
//            Log.i(TAG, "SATODEBUG SatochipHWWallet getXpubs() after cardManager start");
//
//            nfcAdapter = NfcAdapter.getDefaultAdapter(this.context);
//            nfcAdapter.enableReaderMode(
//                    activity,
//                    cardManager,
//                    NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_NFC_B | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
//                    null
//            );
//
//            Log.i(TAG, "SATODEBUG SatochipHWWallet getXpubs() after NfcAdapter.enableReaderMode");


            // poll for result from cardListener onConnected
//            Log.i(TAG, "SATODEBUG SatochipHWWallet getXpubs() before SLEEP");
//            TimeUnit.SECONDS.sleep(10);
//            Log.i(TAG, "SATODEBUG SatochipHWWallet getXpubs() after SLEEP");
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

//        final List<String> xpubs = new ArrayList<>(paths.size());
//
//        // TODO
//        xpubs.add("xpub6EPNojiyVmkzkEMmkwfjVc4YdyBXoQH8QwdifcPaKqVszSyg6oczSEgfP5AYUUs5hNG9kNfosAbTSLZqjEfMGdA85F7dx3kk6qDFP6va7mz");
//        if (paths.size()>=2) {
//            xpubs.add("xpub69CqtDngRoBFv667kKodkCxvGegsJZwVUTmE8K46e3HKL5JVYL4ZpZZSBvosZPdgVmFxa3fFvkYxYfXBQyYTkME99wHhugHAARLECJe64Ee");
//        }
//        if (paths.size()>=3) {
//            xpubs.add("xpub69CqtDngRoBFv667kKodkCxvGegsJZwVUTmE8K46e3HKL5JVYL4ZpZZSBvosZPdgVmFxa3fFvkYxYfXBQyYTkME99wHhugHAARLECJe64Ee");
//        }
//
//        Log.i(TAG, "SATODEBUG SatochipHWWallet getXpubs() before RETURN");
//
//        return xpubs;
    }

    @NonNull
    @Override
    public SignMessageResult signMessage(@NonNull List<Integer> path, @NonNull String message, boolean useAeProtocol, @Nullable String aeHostCommitment, @Nullable String aeHostEntropy, @Nullable HardwareWalletInteraction hwInteraction) {
        Log.i("SatochipHWWallet", "signMessage start");
        Log.i("SatochipHWWallet", "signMessage start path: " + path);
        Log.i("SatochipHWWallet", "signMessage start useAeProtocol: " + useAeProtocol);
        Log.i("SatochipHWWallet", "signMessage start aeHostCommitment: " + aeHostCommitment);
        Log.i("SatochipHWWallet", "signMessage start aeHostEntropy: " + aeHostEntropy);
        Log.i("SatochipHWWallet", "signMessage start hwInteraction: " + hwInteraction);

        if (useAeProtocol) {
            throw new RuntimeException("Hardware Wallet does not support the Anti-Exfil protocol");
        }

        //TODO
        String signature = "aabbccddee";
        return new SignMessageResult(signature, null);
    }

    @NonNull
    @Override
    public SignTransactionResult signTransaction(@NonNull Network network, @NonNull String transaction, @NonNull List<InputOutput> inputs, @NonNull List<InputOutput> outputs, @Nullable Map<String, String> transactions, boolean useAeProtocol, @Nullable HardwareWalletInteraction hwInteraction) {
        Log.i("SatochipHWWallet", "signTransaction start");
        if(network.isLiquid()){
            throw new RuntimeException(network.getCanonicalName() + " is not supported");
        }
        try {

            if (useAeProtocol) {
                throw new RuntimeException("Hardware Wallet does not support the Anti-Exfil protocol");
            }

            return signTransactionImpl(network, hwInteraction, transaction, inputs, outputs, transactions);
        } finally {
            // Free all wally txs to ensure we don't leak any memory
            for (Map.Entry<String, Object> entry : mPrevTxs.entrySet()) {
                Wally.tx_free(entry.getValue());
            }
            mPrevTxs.clear();
        }
    }

    private synchronized SignTransactionResult signTransactionImpl(final Network network,
                                                                   @Nullable HardwareWalletInteraction hwInteraction,
                                                                   final String transaction,
                                                                   final List<InputOutput> inputs,
                                                                   final List<InputOutput> outputs,
                                                                   final Map<String, String> transactions)
    {
        Log.i("SatochipHWWallet", "SignTransactionImpl start");
        final String[] signatures = new String[inputs.size()];

        final byte[] txBytes = Wally.hex_to_bytes(transaction);
        final Object wallytx = Wally.tx_from_bytes(txBytes, Wally.WALLY_TX_FLAG_USE_WITNESS);

        final int txVersion = Wally.tx_get_version(wallytx);
        final int txLocktime = Wally.tx_get_locktime(wallytx);

        if (transactions != null) {
            for (Map.Entry<String, String> t : transactions.entrySet())
                mPrevTxs.put(t.getKey(), Wally.tx_from_hex(t.getValue(), Wally.WALLY_TX_FLAG_USE_WITNESS));
        }

        // todo
        return new SignTransactionResult(Arrays.asList(signatures), null);

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



    private static List<Integer> getIntegerPath(final List<Long> unsigned) {
        //return unsigned.stream().map(Long::intValue).collect(Collectors.toList());
        final List<Integer> signed = new ArrayList<>(unsigned.size());
        for (final Long n : unsigned) {
            signed.add(n.intValue());
        }
        return signed;
    }

    private static Integer unharden(final Integer i) {
        return Integer.MIN_VALUE + i;
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
        //result.write((byte)depth);
        for (final Integer element : path) {
            BufferUtils.writeUint32BE(result, element);
        }
        Bip32Path bip32Path = new Bip32Path(depth, result.toByteArray());
        return bip32Path;
    }


}
