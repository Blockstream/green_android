package com.greenaddress.greenbits.wallets;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.blockstream.common.devices.ApduException;
import com.blockstream.common.devices.ApduResponse;
import com.blockstream.common.devices.ApplicationStatus;
import com.blockstream.common.devices.CardChannel;
import com.blockstream.common.devices.DeviceModel;
import com.blockstream.common.devices.SatochipCommandSet;
import com.blockstream.common.extensions.GdkExtensionsKt;
import com.blockstream.common.gdk.data.Account;
import com.blockstream.common.gdk.data.AccountType;
import com.blockstream.common.gdk.data.Device;
import com.blockstream.common.gdk.data.InputOutput;
import com.blockstream.common.gdk.data.Network;
import com.blockstream.common.gdk.device.BlindingFactorsResult;
import com.blockstream.common.devices.DeviceBrand;
import com.blockstream.common.gdk.device.GdkHardwareWallet;
import com.blockstream.common.gdk.device.HardwareWalletInteraction;
import com.blockstream.common.gdk.device.SignMessageResult;
import com.blockstream.common.gdk.device.SignTransactionResult;
import com.blockstream.libwally.Wally;
import com.google.common.base.Joiner;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.satoshilabs.trezor.Trezor;
import com.satoshilabs.trezor.protobuf.TrezorMessage;
import com.satoshilabs.trezor.protobuf.TrezorType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kotlinx.coroutines.CompletableDeferred;
import kotlinx.coroutines.CompletableDeferredKt;
import kotlinx.coroutines.flow.MutableStateFlow;


public class SatochipHWWallet extends GdkHardwareWallet {

    private static final String TAG = SatochipHWWallet.class.getSimpleName();

    private final Map<String, TrezorType.HDNodeType> mUserXPubs = new HashMap<>();
    private final Map<String, TrezorType.HDNodeType> mServiceXPubs = new HashMap<>();
    private final Map<String, TrezorType.HDNodeType> mRecoveryXPubs = new HashMap<>();
    private final Map<String, Object> mPrevTxs = new HashMap<>();

    private final Device device;
    private final DeviceModel model;
    private final String firmwareVersion = "x.y-z.w"; //todo

    private final CardChannel channel = null;
    private final SatochipCommandSet cmdSet;

    public SatochipHWWallet(Device device, CardChannel channel) {
        Log.i("SatochipHWWallet", "constructor start");
        //mTrezor = t;
        this.device = device;
        String model = "Satochip";
        this.model = DeviceModel.SatochipGeneric;

        this.cmdSet = new SatochipCommandSet(channel);

    }

    @Override
    public synchronized void disconnect() {
        Log.i("SatochipHWWallet", "disconnect start");
        // No-op
    }

    @NonNull
    @Override
    public synchronized List<String> getXpubs(@NonNull Network network, @NonNull List<? extends List<Integer>> paths, @Nullable HardwareWalletInteraction hwInteraction) {
        Log.i("SatochipHWWallet", "getXpubs start");
        Log.i("SatochipHWWallet", "getXpubs paths: " + paths);
        Log.i("SatochipHWWallet", "getXpubs HardwareWalletInteraction: " + hwInteraction);

        // debug
        try {
            ApduResponse rapduSelect = cmdSet.cardSelect("satochip").checkOK();
            ApduResponse rapduStatus = cmdSet.cardGetStatus();//To update status if it's not the first reading
            ApplicationStatus cardStatus = cmdSet.getApplicationStatus(); //applicationStatus ?: return
            Log.i("SatochipHWWallet", "getXpubs readCard cardStatus: $cardStatus");
            Log.i("SatochipHWWallet", "getXpubs readCard cardStatus: ${cardStatus.toString()}");
        } catch (Exception e) {
            Log.i("SatochipHWWallet", "getXpubs exception: " + e);
        }

        final List<String> xpubs = new ArrayList<>(paths.size());

        // TODO
        xpubs.add("xpub6EPNojiyVmkzkEMmkwfjVc4YdyBXoQH8QwdifcPaKqVszSyg6oczSEgfP5AYUUs5hNG9kNfosAbTSLZqjEfMGdA85F7dx3kk6qDFP6va7mz");
        if (paths.size()>=2) {
            xpubs.add("xpub69CqtDngRoBFv667kKodkCxvGegsJZwVUTmE8K46e3HKL5JVYL4ZpZZSBvosZPdgVmFxa3fFvkYxYfXBQyYTkME99wHhugHAARLECJe64Ee");
        }
        if (paths.size()>=3) {
            xpubs.add("xpub69CqtDngRoBFv667kKodkCxvGegsJZwVUTmE8K46e3HKL5JVYL4ZpZZSBvosZPdgVmFxa3fFvkYxYfXBQyYTkME99wHhugHAARLECJe64Ee");
        }
        return xpubs;
    }

    @NonNull
    @Override
    public SignMessageResult signMessage(@NonNull List<Integer> path, @NonNull String message, boolean useAeProtocol, @Nullable String aeHostCommitment, @Nullable String aeHostEntropy, @Nullable HardwareWalletInteraction hwInteraction) {
        Log.i("SatochipHWWallet", "signMessage start");
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
}
