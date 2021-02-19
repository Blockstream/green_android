package com.greenaddress.greenbits.wallets;

import com.blockstream.hardware.R;
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
import com.greenaddress.greenapi.HWWalletBridge;
import com.greenaddress.greenapi.HWWallet;
import com.greenaddress.greenapi.data.HWDeviceData;
import com.greenaddress.greenapi.data.InputOutputData;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenapi.data.SubaccountData;

import org.bitcoinj.core.VarInt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import static com.greenaddress.greenapi.data.InputOutputData.reverseBytes;


public class BTChipHWWallet extends HWWallet {
    private static final byte SIGHASH_ALL = 1;

    private static final ListeningExecutorService mExecutor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(
                                                                                                   1));
    private final BTChipDongle mDongle;
    private final String mPin;
    private final Map<String, String> mUserXPubs = new HashMap<>();

    public BTChipHWWallet(final BTChipDongle dongle, final String pin,
                          final SettableFuture<Integer> remainingAttemptsFuture, final NetworkData network,
                          final HWDeviceData hwDeviceData) {
        mDongle = dongle;
        mPin = pin;
        mNetwork = network;
        mHWDeviceData = hwDeviceData;
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

    @Override
    public void disconnect() {
        // No-op
    }

    public List<String> getXpubs(final HWWalletBridge parent, final List<List<Integer>> paths) {
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

    @Override
    public String getBlindingKey(final HWWalletBridge parent, final String scriptHex) {
        try {
            final BTChipDongle.BTChipPublicKey blindingKey = mDongle.getBlindingKey(Wally.hex_to_bytes(scriptHex));
            final byte[] compressed = KeyUtils.compressPublicKey(blindingKey.getPublicKey());

            return Wally.hex_from_bytes(compressed);
        } catch (final Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public String getBlindingNonce(HWWalletBridge parent, String pubkey, String scriptHex) {
        try {
            final byte[] fullPk = Wally.ec_public_key_decompress(Wally.hex_to_bytes(pubkey), null);
            final BTChipDongle.BTChipPublicKey nonce = mDongle.getBlindingNonce(fullPk, Wally.hex_to_bytes(scriptHex));
            return Wally.hex_from_bytes(nonce.getPublicKey());
        } catch (final Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public String getGreenAddress(final SubaccountData subaccount, final long branch, final long pointer,
                                  final long csvBlocks) throws BTChipException {
        return mDongle.getGreenAddress(csvBlocks > 0, subaccount.getPointer(), branch, pointer, csvBlocks);
    }

    public String signMessage(final HWWalletBridge parent, final List<Integer> path, final String message) {
        try {
            mDongle.signMessagePrepare(path, message.getBytes(StandardCharsets.UTF_8));
            return Wally.hex_from_bytes(mDongle.signMessageSign(new byte[] {0}).getSignature());
        } catch (final Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public List<String> signTransaction(final HWWalletBridge parent, final ObjectNode tx,
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

            final List<byte[]> swSigs = sw ? signSW(parent, tx, inputs, outputs, transactions) : new ArrayList<>();
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

    @Override
    public LiquidHWResult signLiquidTransaction(HWWalletBridge parent, ObjectNode tx, List<InputOutputData> inputs,
                                                List<InputOutputData> outputs, Map<String, String> transactions,
                                                List<String> addressTypes) {
        final HashSet<String> addrTypes = new HashSet<>(addressTypes);
        try {
            if (addrTypes.contains("p2pkh"))
                throw new RuntimeException("Hardware Wallet cannot sign sweep inputs");

            // Sanity check on the firmware version, in case devices have been swapped
            if (!mDongle.shouldUseNewSigningApi())
                throw new RuntimeException("Segwit not supported");

            final LiquidSigCommitment resp = signLiquid(parent, tx, inputs, outputs);

            // TODO: refactor using map or something like that
            final List<String> assetCommitments = new ArrayList<>(outputs.size());
            final List<String> valueCommitments = new ArrayList<>(outputs.size());
            final List<String> amountBlinders = new ArrayList<>(outputs.size());
            final List<String> assetBlinders = new ArrayList<>(outputs.size());
            for (int i = 0; i < outputs.size(); i++) {
                if (resp.getAssetCommitments().get(i) == null) {
                    assetCommitments.add(null);
                    valueCommitments.add(null);
                    assetBlinders.add(null);
                    amountBlinders.add(null);
                    continue;
                }

                assetCommitments.add(Wally.hex_from_bytes(resp.getAssetCommitments().get(i)));
                valueCommitments.add(Wally.hex_from_bytes(resp.getValueCommitments().get(i)));
                assetBlinders.add(Wally.hex_from_bytes(reverseBytes(resp.getAbfs().get(i))));
                amountBlinders.add(Wally.hex_from_bytes(reverseBytes(resp.getVbfs().get(i))));
            }

            final List<String> sigs = new ArrayList<>(inputs.size());
            for (final byte[] sig : resp.getSignatures()) {
                sigs.add(Wally.hex_from_bytes(sig));
            }

            return new LiquidHWResult(sigs, assetCommitments, valueCommitments, assetBlinders, amountBlinders);
        } catch (final BTChipException e) {
            e.printStackTrace();
            throw new RuntimeException("Signing Error: " + e.getMessage());
        }
    }

    public static class LiquidSigCommitment {
        private final List<byte[]> signatures;
        private final List<byte[]> assetCommitments;
        private final List<byte[]> valueCommitments;
        private final List<byte[]> abfs;
        private final List<byte[]> vbfs;

        public LiquidSigCommitment(List<byte[]> signatures, List<byte[]> assetCommitments,
                                   List<byte[]> valueCommitments, List<byte[]> abfs, List<byte[]> vbfs) {
            this.signatures = signatures;
            this.assetCommitments = assetCommitments;
            this.valueCommitments = valueCommitments;
            this.abfs = abfs;
            this.vbfs = vbfs;
        }

        public List<byte[]> getSignatures() {
            return signatures;
        }

        public List<byte[]> getAssetCommitments() {
            return assetCommitments;
        }

        public List<byte[]> getValueCommitments() {
            return valueCommitments;
        }

        public List<byte[]> getAbfs() {
            return abfs;
        }

        public List<byte[]> getVbfs() {
            return vbfs;
        }
    }

    // This assumes segwit = true
    private LiquidSigCommitment signLiquid(final HWWalletBridge parent, final ObjectNode tx,
                                           final List<InputOutputData> inputs,
                                           final List<InputOutputData> outputs) throws BTChipException {
        final BTChipDongle.BTChipLiquidInput hwInputs[] = new BTChipDongle.BTChipLiquidInput[inputs.size()];

        for (int i = 0; i < hwInputs.length; ++i) {
            final InputOutputData in = inputs.get(i);
            hwInputs[i] = mDongle.createLiquidInput(inputLiquidBytes(in), sequenceBytes(in));
        }

        // Prepare the pseudo transaction
        // Provide the first script instead of a null script to initialize the P2SH confirmation logic
        final long version = tx.get("transaction_version").asLong();
        final byte script0[] = Wally.hex_to_bytes(inputs.get(0).getPrevoutScript());
        mDongle.startUntrustedLiquidTransaction(version, true, 0, hwInputs, script0);

        if (mDongle.supportScreen()) {
            parent.interactionRequest(this);
        }

        List<Long> inputValues = new ArrayList<>();
        List<byte[]> abfs = new ArrayList<>();
        List<byte[]> vbfs = new ArrayList<>();

        for (InputOutputData in : inputs) {
            inputValues.add(in.getSatoshi());
            abfs.add(in.getAbfs());
            vbfs.add(in.getVbfs());
        }

        List <BTChipDongle.BTChipLiquidTrustedCommitments> commitments = mDongle.getLiquidCommitments(inputValues, abfs, vbfs, inputs.size(), outputs);

        mDongle.finalizeLiquidInputFull(outputLiquidBytes(outputs, commitments));
        mDongle.provideLiquidIssuanceInformation(inputs.size());

        final long locktime = tx.get("transaction_locktime").asLong();

        // Sign each input
        final BTChipDongle.BTChipLiquidInput singleInput[] = new BTChipDongle.BTChipLiquidInput[1];

        final List<byte[]> sigs = new ArrayList<>(hwInputs.length);
        final List<byte[]> assetCommitents = new ArrayList<>(outputs.size());
        final List<byte[]> valueCommitents = new ArrayList<>(outputs.size());

        for (int i = 0; i < outputs.size(); i++) {
            if (commitments.get(i) == null) {
                assetCommitents.add(null);
                valueCommitents.add(null);
                continue;
            }

            final byte[] aComm = new byte[33];
            final byte[] vComm = new byte[33];
            System.arraycopy(commitments.get(i).getAssetCommitment(), 0, aComm, 0, 33);
            System.arraycopy(commitments.get(i).getValueCommitment(), 0, vComm, 0, 33);

            assetCommitents.add(aComm);
            valueCommitents.add(vComm);
        }

        for (int i = 0; i < hwInputs.length; ++i) {
            final InputOutputData in = inputs.get(i);
            singleInput[0] = hwInputs[i];
            final byte script[] = Wally.hex_to_bytes(in.getPrevoutScript());

            mDongle.startUntrustedLiquidTransaction(version, false, 0, singleInput, script);
            sigs.add(mDongle.untrustedLiquidHashSign(in.getUserPathAsInts(), locktime, SIGHASH_ALL));
        }

        // remove the inputs from assetBlinders/amountBlinders
        for (int i = 0; i < inputs.size(); i++) {
            abfs.remove(0);
            vbfs.remove(0);
        }

        return new LiquidSigCommitment(sigs, assetCommitents, valueCommitents, abfs, vbfs);
    }

    // Helper to get the hw inputs
    private BTChipDongle.BTChipInput[] getHwInputs(final List<InputOutputData> inputs,
                                                   final Map<String, String> transactions,
                                                   final boolean segwit) throws BTChipException {
        final BTChipDongle.BTChipInput[] hwInputs = new BTChipDongle.BTChipInput[inputs.size()];
        final boolean preferTrustedInputs = !segwit || mDongle.shouldUseTrustedInputForSegwit();

        if (preferTrustedInputs && mDongle.supportScreen()) {
            for (int i = 0; i < hwInputs.length; ++i) {
                final InputOutputData in = inputs.get(i);
                final String txHex = transactions.get(in.getTxhash());
                if (txHex == null)
                    throw new BTChipException(String.format("previous transaction %s not found", in.getTxhash()));

                final ByteArrayInputStream is = new ByteArrayInputStream(Wally.hex_to_bytes(txHex));
                hwInputs[i] = mDongle.getTrustedInput(new BitcoinTransaction(is), in.getPtIdx(),
                                                      in.getSequence(), segwit);
            }
        } else {
            for (int i = 0; i < hwInputs.length; ++i) {
                final InputOutputData in = inputs.get(i);
                hwInputs[i] = mDongle.createInput(inputBytes(in, segwit), sequenceBytes(in), false, segwit);
            }
        }

        return hwInputs;
    }

    private List<byte[]> signSW(final HWWalletBridge parent, final ObjectNode tx,
                                final List<InputOutputData> inputs,
                                final List<InputOutputData> outputs,
                                final Map<String, String> transactions) throws BTChipException {
        final BTChipDongle.BTChipInput[] hwInputs = getHwInputs(inputs, transactions, true);

        // Prepare the pseudo transaction
        // Provide the first script instead of a null script to initialize the P2SH confirmation logic
        final long version = tx.get("transaction_version").asLong();
        final byte[] script0 = Wally.hex_to_bytes(inputs.get(0).getPrevoutScript());
        mDongle.startUntrustedTransaction(version, true, 0, hwInputs, script0, true);

        if (mDongle.supportScreen()) {
            parent.interactionRequest(this);
        }
        mDongle.finalizeInputFull(outputBytes(outputs));

        final long locktime = tx.get("transaction_locktime").asLong();

        // Sign each input
        final BTChipDongle.BTChipInput[] singleInput = new BTChipDongle.BTChipInput[1];
        final List<byte[]> sigs = new ArrayList<>(hwInputs.length);

        for (int i = 0; i < hwInputs.length; ++i) {
            final InputOutputData in = inputs.get(i);
            if (in.getAddressType().equals("p2sh"))
                continue; // sign segwit only
            singleInput[0] = hwInputs[i];
            final byte[] script = Wally.hex_to_bytes(in.getPrevoutScript());
            mDongle.startUntrustedTransaction(version, false, 0, singleInput, script, true);
            sigs.add(mDongle.untrustedHashSign(in.getUserPathAsInts(),"0", locktime, SIGHASH_ALL));
        }
        return sigs;
    }

    private List<byte[]> signNonSW(final HWWalletBridge parent, final ObjectNode tx,
                                   final List<InputOutputData> inputs,
                                   final List<InputOutputData> outputs,
                                   final Map<String, String> transactions) throws BTChipException {
        final BTChipDongle.BTChipInput[] hwInputs = getHwInputs(inputs, transactions, false);

        final long locktime = tx.get("transaction_locktime").asLong();
        final long version = tx.get("transaction_version").asLong();

        final byte[] outputData = outputBytes(outputs);
        final List<byte[]> sigs = new ArrayList<>(hwInputs.length);

        for (int i = 0; i < hwInputs.length; ++i) {
            final InputOutputData in = inputs.get(i);
            final byte[] script = Wally.hex_to_bytes(in.getPrevoutScript());

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
            final byte[] script = Wally.hex_to_bytes(out.getScript());
            putVarInt(os, script.length).write(script, 0, script.length);
        }
        return os.toByteArray();
    }

    private List<byte[]> outputLiquidBytes(final List<InputOutputData> outputs,
                                           final List<BTChipDongle.BTChipLiquidTrustedCommitments> commitments) {
        final List<byte[]> res = new ArrayList<>();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        putVarInt(os, outputs.size());
        res.add(os.toByteArray());

        int index = 0;
        for (final InputOutputData out : outputs) {
            if (out.getScript().length() == 0) { // TODO: should be used for every unblinded output, not only for fees
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

            if (out.getPublicKey() != null) {
                res.add(out.getEphKeypairPubBytes());
                res.add(out.getPublicKeyBytes());
            } else {
                final byte[] nonce = new byte[1];
                res.add(nonce); // one null byte as nonce
                res.add(nonce); // one null byte for the pubkey
            }

            os = new ByteArrayOutputStream(128);
            if (out.getScript().length() == 0) { // fee
                putVarInt(os, 0);
            } else {
                final byte script[] = Wally.hex_to_bytes(out.getScript());
                putVarInt(os, script.length).write(script, 0, script.length);
            }
            res.add(os.toByteArray());

            index++;
        }

        return res;
    }

    private byte[] inputBytes(final InputOutputData in, final boolean isSegwit) {
        final ByteArrayOutputStream os = new ByteArrayOutputStream(32 + (isSegwit ? 12 : 4));
        final byte[] txid = in.getTxid();
        os.write(txid, 0, txid.length);
        BufferUtils.writeUint32LE(os, in.getPtIdx());
        if (isSegwit)
            BufferUtils.writeUint64LE(os, in.getSatoshi());
        return os.toByteArray();
    }

    private byte[] inputLiquidBytes(final InputOutputData in) {
        final ByteArrayOutputStream os = new ByteArrayOutputStream(32 + 4 + 33);
        final byte[] txid = in.getTxid();
        os.write(txid, 0, txid.length);
        BufferUtils.writeUint32LE(os, in.getPtIdx());
        os.write(in.getCommitmentBytes(), 0, in.getCommitmentBytes().length);
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
