package com.greenaddress.greenbits.wallets;

import androidx.annotation.Nullable;

import com.blockstream.JadeHWWallet;
import com.blockstream.gdk.ExtensionsKt;
import com.blockstream.gdk.data.Account;
import com.blockstream.gdk.data.AccountType;
import com.blockstream.gdk.data.Device;
import com.blockstream.gdk.data.InputOutput;
import com.blockstream.gdk.data.Network;
import com.blockstream.hardware.R;
import com.blockstream.libwally.Wally;
import com.btchip.BTChipConstants;
import com.btchip.BTChipDongle;
import com.btchip.BTChipException;
import com.btchip.BitcoinTransaction;
import com.btchip.comm.android.BTChipTransportAndroid;
import com.btchip.comm.android.BTChipTransportAndroidHID;
import com.btchip.utils.BufferUtils;
import com.btchip.utils.KeyUtils;
import com.btchip.utils.VarintUtils;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Joiner;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.greenaddress.greenapi.HWWallet;
import com.greenaddress.greenapi.HWWalletBridge;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import io.reactivex.rxjava3.subjects.PublishSubject;
import kotlinx.serialization.json.JsonElement;


public class BTChipHWWallet extends HWWallet {
    private static final byte SIGHASH_ALL = 1;

    private static final ListeningExecutorService mExecutor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(
                                                                                                   1));
    private final BTChipDongle mDongle;
    private final String mPin;
    private final Map<String, String> mUserXPubs = new HashMap<>();
    private final PublishSubject<Boolean> mBleDisconnectEvent;

    protected Network mNetwork;

    public BTChipHWWallet(final BTChipDongle dongle, final String pin,
                          final Network network,
                          @Nullable final Device device,
                          final String firmwareVersion,
                          PublishSubject<Boolean> bleDisconnectEvent) {
        mDongle = dongle;
        mPin = pin;
        mNetwork = network;
        mDevice = device;
        if(dongle.getTransport().isUsb()){
            if(dongle.getTransport() instanceof BTChipTransportAndroidHID){
                if(BTChipTransportAndroid.isNanoX(((BTChipTransportAndroidHID) dongle.getTransport()).getUsbDevice())){
                    mModel = "Ledger Nano X";
                }else{
                    mModel = "Ledger Nano S";
                }
            }
        } else{
            mModel = "Ledger Nano X";
        }
        mFirmwareVersion = firmwareVersion;

        mBleDisconnectEvent = bleDisconnectEvent;
        if (pin == null)
            return;
        mExecutor.submit(() -> {
            try {
                mDongle.verifyPin(mPin.getBytes());
                // remainingAttemptsFuture.onSuccess(-1);  // -1 means success
            } catch (final BTChipException e) {
                e.printStackTrace();
                final String msg = e.toString();
                final int index = msg.indexOf("63c");
//                if (index != -1)
//                    remainingAttemptsFuture.onSuccess(Integer.valueOf(String.valueOf(msg.charAt(index + 3))));
//                else if (msg.contains("6985"))
//                    // mDongle is not set up
//                    remainingAttemptsFuture.onSuccess(0);
//                else
//                    remainingAttemptsFuture.tryOnError(e);
            } catch (final Exception e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    @Override
    public synchronized void disconnect() {
        // No-op
    }

    public synchronized List<String> getXpubs(final Network network, final HWWalletBridge parent, final List<List<Integer>> paths) {
        final List<String> xpubs = new ArrayList<>(paths.size());
        try {
            for (final List<Integer> path : paths) {
                final String key = Joiner.on("/").join(path);
                if (!mUserXPubs.containsKey(key)) {
                    final BTChipDongle.BTChipPublicKey pubKey = mDongle.getWalletPublicKey(path, false, false);
                    final byte[] compressed = KeyUtils.compressPublicKey(pubKey.getPublicKey());
                    final Object hdkey = Wally.bip32_key_init(network.getVerPublic(), 1 /*FIXME: wally bug*/, 0,
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

    @Override
    public synchronized String getMasterBlindingKey(HWWalletBridge parent) {
        // FIXME: when ledger implement
        return null;
    }

    @Override
    public synchronized String getBlindingKey(final HWWalletBridge parent, final String scriptHex) {
        try {
            final BTChipDongle.BTChipPublicKey blindingKey = mDongle.getBlindingKey(Wally.hex_to_bytes(scriptHex));
            final byte[] compressed = KeyUtils.compressPublicKey(blindingKey.getPublicKey());

            return Wally.hex_from_bytes(compressed);
        } catch (final Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public synchronized String getBlindingNonce(HWWalletBridge parent, String pubkey, String scriptHex) {
        try {
            final byte[] fullPk = Wally.ec_public_key_decompress(Wally.hex_to_bytes(pubkey), null);
            final BTChipDongle.BTChipPublicKey nonce = mDongle.getBlindingNonce(fullPk, Wally.hex_to_bytes(scriptHex));
            return Wally.hex_from_bytes(nonce.getPublicKey());
        } catch (final Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public synchronized BlindingFactorsResult getBlindingFactors(final HWWalletBridge parent, final List<InputOutput> inputs, final List<InputOutput> outputs) {
        try {
            final BTChipDongle.BTChipLiquidInput hwInputs[] = new BTChipDongle.BTChipLiquidInput[inputs.size()];
            for (int i = 0; i < hwInputs.length; ++i) {
                final InputOutput in = inputs.get(i);
                hwInputs[i] = mDongle.createLiquidInput(inputLiquidBytes(in, false), sequenceBytes(in));
            }

            // Prepare the pseudo transaction
            final long version = 2; // irrelevant at this step
            final byte script0[] = new byte[0]; // irrelevant at this step
            mDongle.startUntrustedLiquidTransaction(version, true, 0, hwInputs, script0);

            final BlindingFactorsResult rslt = new BlindingFactorsResult(outputs.size());
            for (int i = 0; i < outputs.size(); ++i) {
                final InputOutput output = outputs.get(i);
                if (output.getBlindingKey() != null) {
                    final byte[] abf = mDongle.getBlindingFactor(i, BTChipConstants.BTCHIP_BLINDING_FACTOR_ASSET);
                    final byte[] vbf = mDongle.getBlindingFactor(i, BTChipConstants.BTCHIP_BLINDING_FACTOR_AMOUNT);
                    rslt.append(Wally.hex_from_bytes(ExtensionsKt.reverseBytes(abf)), Wally.hex_from_bytes(ExtensionsKt.reverseBytes(vbf)));
                } else {
                    // Empty string placeholders
                    rslt.append("", "");
                }
            }
            return rslt;
        } catch (final Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private synchronized static List<Integer> getIntegerPath(final List<Long> unsigned) {
        //return unsigned.stream().map(Long::intValue).collect(Collectors.toList());
        final List<Integer> signed = new ArrayList<>(unsigned.size());
        for (final Long n : unsigned) {
            signed.add(n.intValue());
        }
        return signed;
    }

    @Override
    public synchronized String getGreenAddress(final Network network, HWWalletBridge parent, final Account account, final List<Long> path, final long csvBlocks) throws BTChipException {
        try {
            // Only supported for liquid multisig shield and singlesig btc
            final String address;
            if (network.isSinglesig() && !network.isLiquid()) {
                final BTChipDongle.BTChipPublicKey pubKey = mDongle.getWalletPublicKey(getIntegerPath(path), true, account.getType() == AccountType.BIP84_SEGWIT);
                address = pubKey.getAddress();
            } else if (!network.isSinglesig() && network.isLiquid()) {
                // Green Multisig Shield - pathlen should be 2 for subact 0, and 4 for subact > 0
                // In any case the last two entries are 'branch' and 'pointer'
                final int pathlen = path.size();
                final long branch = path.get(pathlen - 2);
                final long pointer = path.get(pathlen - 1);
                address = mDongle.getGreenAddress(csvBlocks > 0, account.getPointer(), branch, pointer, csvBlocks);
            } else {
                // Unsupported
                address = null;
            }
            return address;
        } catch (final BTChipException e) {
            if (e.getSW() == BTChipConstants.SW_USER_REJECT) {
                // User cancelled on the device - treat as mismatch (rather than error)
                return "";
            }
            throw e;
        }
    }

    @Nullable
    @Override
    public synchronized PublishSubject<Boolean> getBleDisconnectEvent() {
        return mBleDisconnectEvent;
    }

    @Override
    public synchronized SignMsgResult signMessage(final HWWalletBridge parent, final List<Integer> path, final String message,
                                     final boolean useAeProtocol, final String aeHostCommitment, final String aeHostEntropy) {
        if (useAeProtocol) {
            throw new RuntimeException("Hardware Wallet does not support the Anti-Exfil protocol");
        }

        try {
            mDongle.signMessagePrepare(path, message.getBytes(StandardCharsets.UTF_8));
            final String signature = Wally.hex_from_bytes(mDongle.signMessageSign(new byte[] {0}).getSignature());
            return new SignMsgResult(signature, null);
        } catch (final Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public synchronized SignTxResult signTransaction(final Network network, final HWWalletBridge parent, final JsonElement transaction,
                                        final List<InputOutput> inputs,
                                        final List<InputOutput> outputs,
                                        final Map<String, String> transactions,
                                        final boolean useAeProtocol) {
        try {
            final ObjectNode tx = JadeHWWallet.Companion.toObjectNode(transaction);

            if (useAeProtocol) {
                throw new RuntimeException("Hardware Wallet does not support the Anti-Exfil protocol");
            }

            boolean sw = false;
            boolean p2sh = false;
            for (final InputOutput in : inputs) {
                if (in.isSegwit()) {
                    sw = true;
                } else {
                    p2sh = true;
                }
            }

            // Sanity check on the firmware version, in case devices have been swapped
            if (sw && !mDongle.shouldUseNewSigningApi())
                throw new RuntimeException("Segwit not supported");

            final List<byte[]> swSigs = sw ? signSW(parent, tx, inputs, outputs, transactions) : new ArrayList<>();
            final List<byte[]> p2shSigs = p2sh ? signNonSW(parent, tx, inputs, outputs,
                                                           transactions) : new ArrayList<>();

            final List<String> sigs = new ArrayList<>(inputs.size());
            for (final InputOutput in : inputs) {
                final byte[] sig = (in.isSegwit() ? swSigs : p2shSigs).remove(0);
                sigs.add(Wally.hex_from_bytes(sig));
            }
            return new SignTxResult(sigs, null);
        } catch (final BTChipException e) {
            e.printStackTrace();
            throw new RuntimeException("Signing Error: " + e.getMessage());
        }
    }

    @Override
    public synchronized SignTxResult signLiquidTransaction(final Network network, final HWWalletBridge parent, final JsonElement transaction,
                                              final List<InputOutput> inputs,
                                              final List<InputOutput> outputs,
                                              final Map<String, String> transactions,
                                              final boolean useAeProtocol) {
        try {
            final ObjectNode tx = JadeHWWallet.Companion.toObjectNode(transaction);

            if (useAeProtocol) {
                throw new RuntimeException("Hardware Wallet does not support the Anti-Exfil protocol");
            }

            // Sanity check on the firmware version, in case devices have been swapped
            if (!mDongle.shouldUseNewSigningApi())
                throw new RuntimeException("Segwit not supported");

            final BTChipDongle.BTChipLiquidInput hwInputs[] = new BTChipDongle.BTChipLiquidInput[inputs.size()];

            for (int i = 0; i < hwInputs.length; ++i) {
                final InputOutput in = inputs.get(i);
                hwInputs[i] = mDongle.createLiquidInput(inputLiquidBytes(in, true), sequenceBytes(in));
            }

            // Prepare the pseudo transaction
            // Provide the first script instead of a null script to initialize the P2SH confirmation logic
            final long version = tx.get("transaction_version").asLong();
            final byte script0[] = Wally.hex_to_bytes(inputs.get(0).getPrevoutScript());
            mDongle.startUntrustedLiquidTransaction(version, true, 0, hwInputs, script0);

            if (mDongle.supportScreen() && parent != null) {
                parent.interactionRequest(this, null, null);
            }

            // Cannot remove getLiquidCommitments() even though tx already blinded and we should be able to fetch
            // the commitment data from the tx.  The hw signs/hmacs the data in this step, which is verified when
            // the commitments are passed back in for signing.  So, for now at least, this step has to remain.
            final List<Long> inputValues = new ArrayList<>();
            final List<byte[]> abfs = new ArrayList<>();
            final List<byte[]> vbfs = new ArrayList<>();
            for (InputOutput in : inputs) {
                inputValues.add(in.getSatoshi());
                abfs.add(in.getAbfs());
                vbfs.add(in.getVbfs());
            }
            final List<BTChipDongle.BTChipLiquidTrustedCommitments> commitments = mDongle.getLiquidCommitments(inputValues, abfs, vbfs, inputs.size(), outputs);

            // Check commitments are same as we already have in the tx from the blinding step
            // Could be removed/commented or only run in debug mode ?
            final Object wallytx = Wally.tx_from_hex(tx.get("transaction").asText(), Wally.WALLY_TX_FLAG_USE_ELEMENTS);
            if (commitments.size() != outputs.size() || Wally.tx_get_num_outputs(wallytx) != outputs.size()) {
                throw new RuntimeException("outputs/blinders length mismatch");
            }
            for (int i = 0; i < outputs.size(); ++i) {
                final InputOutput output = outputs.get(i);
                if (output.getBlindingKey() != null) {
                    if (!Arrays.equals(Wally.tx_get_output_asset(wallytx, i), commitments.get(i).getAssetCommitment()) ||
                            !Arrays.equals(Wally.tx_get_output_value(wallytx, i), commitments.get(i).getValueCommitment())) {
                        throw new RuntimeException("Output " + i + " blinded commitments mismatch");
                    }
                } else {
                    if (commitments.get(i) != null) {
                        throw new RuntimeException("Output " + i + " unblinded null-commitments mismatch");
                    }
                }
            }
            // end commitments check

            // Prepare for liquid signing, pass commitments
            mDongle.finalizeLiquidInputFull(outputLiquidBytes(outputs, commitments));
            mDongle.provideLiquidIssuanceInformation(inputs.size());

            // Sign each input
            final long locktime = tx.get("transaction_locktime").asLong();
            final List<String> sigs = new ArrayList<>(hwInputs.length);
            for (int i = 0; i < hwInputs.length; ++i) {
                final InputOutput in = inputs.get(i);
                final BTChipDongle.BTChipLiquidInput singleInput[] = { hwInputs[i] };
                final byte[] script = Wally.hex_to_bytes(in.getPrevoutScript());
                mDongle.startUntrustedLiquidTransaction(version, false, 0, singleInput, script);
                final byte[] sig = mDongle.untrustedLiquidHashSign(in.getUserPathAsInts(), locktime, SIGHASH_ALL);
                sigs.add(Wally.hex_from_bytes(sig));
            }

            return new SignTxResult(sigs, null);
        } catch (final BTChipException e) {
            e.printStackTrace();
            throw new RuntimeException("Signing Error: " + e.getMessage());
        }
    }

    // Helper to get the hw inputs
    private BTChipDongle.BTChipInput[] getHwInputs(final List<InputOutput> inputs,
                                                   final Map<String, String> transactions,
                                                   final boolean segwit) throws BTChipException {
        final BTChipDongle.BTChipInput[] hwInputs = new BTChipDongle.BTChipInput[inputs.size()];
        final boolean preferTrustedInputs = !segwit || mDongle.shouldUseTrustedInputForSegwit();

        if (preferTrustedInputs && mDongle.supportScreen()) {
            for (int i = 0; i < hwInputs.length; ++i) {
                final InputOutput in = inputs.get(i);
                final String txHex = transactions.get(in.getTxHash());
                if (txHex == null)
                    throw new BTChipException(String.format("previous transaction %s not found", in.getTxHash()));

                final ByteArrayInputStream is = new ByteArrayInputStream(Wally.hex_to_bytes(txHex));
                hwInputs[i] = mDongle.getTrustedInput(new BitcoinTransaction(is), in.getPtIdxInt(),
                                                      in.getSequenceInt(), segwit);
            }
        } else {
            for (int i = 0; i < hwInputs.length; ++i) {
                final InputOutput in = inputs.get(i);
                hwInputs[i] = mDongle.createInput(inputBytes(in, segwit), sequenceBytes(in), false, segwit);
            }
        }

        return hwInputs;
    }

    private List<byte[]> signSW(final HWWalletBridge parent, final ObjectNode tx,
                                final List<InputOutput> inputs,
                                final List<InputOutput> outputs,
                                final Map<String, String> transactions) throws BTChipException {
        final BTChipDongle.BTChipInput[] hwInputs = getHwInputs(inputs, transactions, true);

        // Prepare the pseudo transaction
        // Provide the first script instead of a null script to initialize the P2SH confirmation logic
        final long version = tx.get("transaction_version").asLong();
        final byte[] script0 = Wally.hex_to_bytes(inputs.get(0).getPrevoutScript());
        mDongle.startUntrustedTransaction(version, true, 0, hwInputs, script0, true);

        if (mDongle.supportScreen() && parent != null) {
            parent.interactionRequest(this, null, null);
        }
        mDongle.finalizeInputFull(outputBytes(outputs));

        final long locktime = tx.get("transaction_locktime").asLong();

        // Sign each input
        final BTChipDongle.BTChipInput[] singleInput = new BTChipDongle.BTChipInput[1];
        final List<byte[]> sigs = new ArrayList<>(hwInputs.length);

        for (int i = 0; i < hwInputs.length; ++i) {
            final InputOutput in = inputs.get(i);
            if (!in.isSegwit())
                continue; // sign segwit only
            singleInput[0] = hwInputs[i];
            final byte[] script = Wally.hex_to_bytes(in.getPrevoutScript());
            mDongle.startUntrustedTransaction(version, false, 0, singleInput, script, true);
            sigs.add(mDongle.untrustedHashSign(in.getUserPathAsInts(),"0", locktime, SIGHASH_ALL));
        }
        return sigs;
    }

    private List<byte[]> signNonSW(final HWWalletBridge parent, final ObjectNode tx,
                                   final List<InputOutput> inputs,
                                   final List<InputOutput> outputs,
                                   final Map<String, String> transactions) throws BTChipException {
        final BTChipDongle.BTChipInput[] hwInputs = getHwInputs(inputs, transactions, false);

        final long locktime = tx.get("transaction_locktime").asLong();
        final long version = tx.get("transaction_version").asLong();

        final byte[] outputData = outputBytes(outputs);
        final List<byte[]> sigs = new ArrayList<>(hwInputs.length);

        for (int i = 0; i < hwInputs.length; ++i) {
            final InputOutput in = inputs.get(i);
            final byte[] script = Wally.hex_to_bytes(in.getPrevoutScript());

            mDongle.startUntrustedTransaction(version, i == 0, i, hwInputs, script, false);
            if (mDongle.supportScreen() && parent != null) {
                parent.interactionRequest(this, null, null);
            }
            mDongle.finalizeInputFull(outputData);

            if (!in.isSegwit()) // sign p2sh/non-segwit only
                sigs.add(mDongle.untrustedHashSign(in.getUserPathAsInts(),"0", locktime, SIGHASH_ALL));
        }
        return sigs;
    }

    private byte[] outputBytes(final List<InputOutput> outputs) {
        final ByteArrayOutputStream os = new ByteArrayOutputStream(outputs.size() * (8 + 256));
        putVarInt(os, outputs.size());
        for (final InputOutput out : outputs) {
            BufferUtils.writeUint64LE(os, out.getSatoshi()); // int64ToByteStreamLE in bitcoinj
            final byte[] script = Wally.hex_to_bytes(out.getScriptPubkey());
            putVarInt(os, script.length).write(script, 0, script.length);
        }
        return os.toByteArray();
    }

    private List<byte[]> outputLiquidBytes(final List<InputOutput> outputs,
                                           final List<BTChipDongle.BTChipLiquidTrustedCommitments> commitments) {
        final List<byte[]> res = new ArrayList<>();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        putVarInt(os, outputs.size());
        res.add(os.toByteArray());

        int index = 0;
        for (final InputOutput out : outputs) {
            if (out.getBlindingKey() == null) {
                os = new ByteArrayOutputStream(33 + 9);
                os.write(0x01);
                os.write(out.getRevertedAssetIdBytes(), 0, 32);
                os.write(0x01);
                BufferUtils.writeUint64BE(os, out.getSatoshi());
                res.add(os.toByteArray());
            } else {
                final byte[] trustedCommitment = commitments.get(index).getDataForFinalize();
                res.add(trustedCommitment);
            }

            if (out.getBlindingKey() != null) {
                res.add(out.getEphKeypairPubBytes());
                res.add(out.getPublicKeyBytes());
            } else {
                final byte[] dummy = new byte[1];
                res.add(dummy); // one null byte as nonce
                res.add(dummy); // one null byte for the pubkey
            }

            os = new ByteArrayOutputStream(128);
            if (out.getScriptPubkey().length() == 0) { // fee
                putVarInt(os, 0);
            } else {
                final byte script[] = Wally.hex_to_bytes(out.getScriptPubkey());
                putVarInt(os, script.length).write(script, 0, script.length);
            }
            res.add(os.toByteArray());

            index++;
        }

        return res;
    }

    private byte[] inputBytes(final InputOutput in, final boolean segwit) {
        final ByteArrayOutputStream os = new ByteArrayOutputStream(32 + (segwit ? 12 : 4));
        final byte[] txid = in.getTxid();
        os.write(txid, 0, txid.length);
        BufferUtils.writeUint32LE(os, in.getPtIdxInt());
        if (segwit)
            BufferUtils.writeUint64LE(os, in.getSatoshi());
        return os.toByteArray();
    }

    private byte[] inputLiquidBytes(final InputOutput in, final boolean strict) {
        final ByteArrayOutputStream os = new ByteArrayOutputStream(32 + 4 + 33);
        final byte[] txid = in.getTxid();
        os.write(txid, 0, txid.length);
        BufferUtils.writeUint32LE(os, in.getPtIdxInt());
        if (strict || in.getCommitment() != null) {
            os.write(in.getCommitmentBytes(), 0, in.getCommitmentBytes().length);
        } else {
            // If commitment data is not available but we are not going to need/use it anyway, we
            // can pass a dummy commitment value here - although it must look like a valid commitment.
            // ie. it must begin 0x08 or 0x09 and be 33 bytes long.
            final byte[] dummy = { 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
            for (int i = 0; i < 3; ++i) {
                os.write(dummy, 0, dummy.length);
            }
        }
        return os.toByteArray();
    }

    private byte[] sequenceBytes(final InputOutput in) {
        final ByteArrayOutputStream os = new ByteArrayOutputStream(4);
        BufferUtils.writeUint32LE(os, in.getSequenceInt());
        return os.toByteArray();
    }

    private ByteArrayOutputStream putVarInt(final ByteArrayOutputStream os, final long v) {
        VarintUtils.write(os, v);
        return os;
    }

    public Network getNetwork() {
        return mNetwork;
    }

    public int getIconResourceId() {
        return R.drawable.ledger_device;
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
