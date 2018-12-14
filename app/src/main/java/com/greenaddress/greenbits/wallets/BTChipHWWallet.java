package com.greenaddress.greenbits.wallets;

import com.blockstream.libwally.Wally;
import com.btchip.BTChipDongle;
import com.btchip.BTChipException;
import com.btchip.BitcoinTransaction;
import com.btchip.utils.BufferUtils;
import com.btchip.utils.KeyUtils;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Joiner;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.greenaddress.greenapi.HWWallet;
import com.greenaddress.greenapi.data.InputOutputData;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenbits.ui.GaActivity;
import com.greenaddress.greenbits.ui.R;

import org.bitcoinj.core.VarInt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;


public class BTChipHWWallet extends HWWallet {
    private static final byte SIGHASH_ALL = 1;

    private static final ListeningExecutorService mExecutor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(
                                                                                                   1));
    private final BTChipDongle mDongle;
    private final String mPin;
    private final Map<String, String> mUserXPubs = new HashMap<>();

    public BTChipHWWallet(final BTChipDongle dongle, final String pin,
                          final SettableFuture<Integer> remainingAttemptsFuture, final NetworkData network) {
        mDongle = dongle;
        mPin = pin;
        mNetwork = network;
        if (pin == null)
            return;
        mExecutor.submit(() -> {
            try {
                mDongle.verifyPin(mPin.getBytes());
                remainingAttemptsFuture.set(-1);  // -1 means success
            } catch (final BTChipException e) {
                e.printStackTrace();
                final String msg = e.toString();
                final int index = msg.indexOf("63c");
                if (index != -1)
                    remainingAttemptsFuture.set(Integer.valueOf(String.valueOf(msg.charAt(index + 3))));
                else if (msg.contains("6985"))
                    // mDongle is not set up
                    remainingAttemptsFuture.set(0);
                else
                    remainingAttemptsFuture.setException(e);
            } catch (final Exception e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    public List<String> getXpubs(final GaActivity parent, final List<List<Integer>> paths) {
        final List<String> xpubs = new ArrayList<>(paths.size());
        try {
            for (final List<Integer> path : paths) {
                final String key = Joiner.on("/").join(path);
                if (!mUserXPubs.containsKey(key)) {
                    final BTChipDongle.BTChipPublicKey pubKey = mDongle.getWalletPublicKey(path);
                    final byte[] compressed = KeyUtils.compressPublicKey(pubKey.getPublicKey());
                    final Object hdkey = Wally.bip32_key_init(mNetwork.getVerPublic(), 1 /*FIXME: wally bug*/, 0,
                                                              pubKey.getChainCode(), compressed,null, null, null);
                    mUserXPubs.put(key, Wally.bip32_key_to_base58(hdkey, Wally.BIP32_FLAG_KEY_PUBLIC));
                    Wally.bip32_key_free(hdkey);
                }
                xpubs.add(mUserXPubs.get(key));
            }
            return xpubs;
        } catch (final Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public String signMessage(final GaActivity parent, final List<Integer> path, final String message) {
        try {
            mDongle.signMessagePrepare(path, message.getBytes("UTF-8"));
            return Wally.hex_from_bytes(mDongle.signMessageSign(new byte[] {0}).getSignature());
        } catch (final Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public List<String> signTransaction(final GaActivity parent, final ObjectNode tx,
                                        final List<InputOutputData> inputs,
                                        final List<InputOutputData> outputs,
                                        final Map<String, String> transactions,
                                        final List<String> addressTypes) {
        final HashSet<String> addrTypes = new HashSet<>(addressTypes);
        try {
            final boolean sw = addrTypes.contains("p2wsh") || addrTypes.contains("csv");
            final boolean p2sh = addrTypes.contains("p2sh");

            if (addrTypes.contains("p2pkh"))
                throw new RuntimeException("Hardware Wallet cannot sign sweep inputs");

            // Sanity check on the firmware version, in case devices have been swapped
            if (sw && !mDongle.shouldUseNewSigningApi())
                throw new RuntimeException("Segwit not supported");

            final List<byte[]> swSigs = sw ? signSW(parent, tx, inputs, outputs) : new ArrayList<>();
            final List<byte[]> p2shSigs = p2sh ? signNonSW(parent, tx, inputs, outputs,
                                                           transactions) : new ArrayList<>();

            final List<String> sigs = new ArrayList<>(inputs.size());
            for (final InputOutputData in : inputs) {
                final boolean swInput = !in.getAddressType().equals("p2sh");
                final byte[] sig = (swInput ? swSigs : p2shSigs).remove(0);
                sigs.add(Wally.hex_from_bytes(sig));
            }
            return sigs;
        } catch (final BTChipException e) {
            e.printStackTrace();
            throw new RuntimeException("Signing Error: " + e.getMessage());
        }
    }

    private List<byte[]> signSW(final GaActivity parent, final ObjectNode tx,
                                final List<InputOutputData> inputs,
                                final List<InputOutputData> outputs) throws BTChipException {
        final BTChipDongle.BTChipInput hwInputs[] = new BTChipDongle.BTChipInput[inputs.size()];

        for (int i = 0; i < hwInputs.length; ++i) {
            final InputOutputData in = inputs.get(i);
            hwInputs[i] = mDongle.createInput(inputBytes(in, true), sequenceBytes(in), false, true);
        }

        // Prepare the pseudo transaction
        // Provide the first script instead of a null script to initialize the P2SH confirmation logic
        final long version = tx.get("transaction_version").asLong();
        final byte script0[] = Wally.hex_to_bytes(inputs.get(0).getPrevoutScript());
        mDongle.startUntrustedTransaction(version, true, 0, hwInputs, script0, true);

        if (mDongle.supportScreen()) {
            parent.interactionRequest(this);
        }
        mDongle.finalizeInputFull(outputBytes(outputs));

        final long locktime = tx.get("transaction_locktime").asLong();

        // Sign each input
        final BTChipDongle.BTChipInput singleInput[] = new BTChipDongle.BTChipInput[1];
        final List<byte[]> sigs = new ArrayList<>(hwInputs.length);

        for (int i = 0; i < hwInputs.length; ++i) {
            final InputOutputData in = inputs.get(i);
            if (in.getAddressType().equals("p2sh"))
                continue; // sign segwit only
            singleInput[0] = hwInputs[i];
            final byte script[] = Wally.hex_to_bytes(in.getPrevoutScript());
            mDongle.startUntrustedTransaction(version, false, 0, singleInput, script, true);
            sigs.add(mDongle.untrustedHashSign(in.getUserPathAsInts(),"0", locktime, SIGHASH_ALL));
        }
        return sigs;
    }

    private List<byte[]> signNonSW(final GaActivity parent, final ObjectNode tx,
                                   final List<InputOutputData> inputs,
                                   final List<InputOutputData> outputs,
                                   final Map<String, String> transactions) throws BTChipException {

        final BTChipDongle.BTChipInput hwInputs[] = new BTChipDongle.BTChipInput[inputs.size()];

        if (!mDongle.supportScreen()) {
            for (int i = 0; i < hwInputs.length; ++i) {
                final InputOutputData in = inputs.get(i);
                hwInputs[i] = mDongle.createInput(inputBytes(in, false), sequenceBytes(in), false, false);
            }
        } else {
            for (int i = 0; i < hwInputs.length; ++i) {
                final InputOutputData in = inputs.get(i);

                final String txHex = transactions.get(in.getTxhash());
                if (txHex == null)
                    throw new BTChipException(String.format("previous transaction %s not found", in.getTxhash()));
                final ByteArrayInputStream is = new ByteArrayInputStream(Wally.hex_to_bytes(txHex));
                hwInputs[i] = mDongle.getTrustedInput(new BitcoinTransaction(is), in.getPtIdx(), in.getSequence());
            }
        }

        final long locktime = tx.get("transaction_locktime").asLong();
        final long version = tx.get("transaction_version").asLong();

        final byte[] outputData = outputBytes(outputs);
        final List<byte[]> sigs = new ArrayList<>(hwInputs.length);

        for (int i = 0; i < hwInputs.length; ++i) {
            final InputOutputData in = inputs.get(i);
            final byte script[] = Wally.hex_to_bytes(in.getPrevoutScript());

            mDongle.startUntrustedTransaction(version, i == 0, i, hwInputs, script, false);
            if (mDongle.supportScreen()) {
                parent.interactionRequest(this);
            }
            mDongle.finalizeInputFull(outputData);

            if (in.getAddressType().equals("p2sh")) // sign p2sh/non-segwit only
                sigs.add(mDongle.untrustedHashSign(in.getUserPathAsInts(),"0", locktime, SIGHASH_ALL));
        }
        return sigs;
    }

    private byte[] outputBytes(final List<InputOutputData> outputs) {
        final ByteArrayOutputStream os = new ByteArrayOutputStream(outputs.size() * (8 + 256));
        putVarInt(os, outputs.size());
        for (final InputOutputData out : outputs) {
            BufferUtils.writeUint64LE(os, out.getSatoshi()); // int64ToByteStreamLE in bitcoinj
            final byte script[] = Wally.hex_to_bytes(out.getScript());
            putVarInt(os, script.length).write(script, 0, script.length);
        }
        return os.toByteArray();
    }

    private byte[] inputBytes(final InputOutputData in, boolean isSegwit) {
        final ByteArrayOutputStream os = new ByteArrayOutputStream(32 + (isSegwit ? 12 : 4));
        final byte[] txid = in.getTxid();
        os.write(txid, 0, txid.length);
        BufferUtils.writeUint32LE(os, in.getPtIdx());
        if (isSegwit)
            BufferUtils.writeUint64LE(os, in.getSatoshi());
        return os.toByteArray();
    }

    private byte[] sequenceBytes(final InputOutputData in) {
        final ByteArrayOutputStream os = new ByteArrayOutputStream(4);
        BufferUtils.writeUint32LE(os, in.getSequence());
        return os.toByteArray();
    }

    private ByteArrayOutputStream putVarInt(final ByteArrayOutputStream os, final long v) {
        final byte[] buf = new VarInt(v).encode();
        os.write(buf, 0, buf.length);
        return os;
    }

    public int getIconResourceId() {
        return R.drawable.ic_ledger;
    }

    /*
       public boolean checkConnected() {
        try {
                Log.d(TAG, "Connection check");
                mDongle.getFirmwareVersion();
                Log.d(TAG, "Connection ok");
                return true;
        }
        catch(final Exception e) {
                Log.d(TAG, "Connection not connected");
                try {
                        mDongle.getTransport().close();
                        Log.d(TAG, "Connection closed");
                }
                catch(final Exception e1) {
                }
                return false;
        }
       }

       public void setTransport(final BTChipTransport transport) {
        mDongle.setTransport(transport);
        try {
                mDongle.verifyPin(mPin.getBytes());
        }
        catch(final Exception e) {
        }
       }
     */
}
