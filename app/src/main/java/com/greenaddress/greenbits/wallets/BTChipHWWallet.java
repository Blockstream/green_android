package com.greenaddress.greenbits.wallets;

import android.util.Log;

import com.blockstream.libwally.Wally;
import com.btchip.BTChipDongle;
import com.btchip.BTChipException;
import com.btchip.BitcoinTransaction;
import com.btchip.comm.BTChipTransport;
import com.btchip.utils.BufferUtils;
import com.google.common.base.Joiner;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.greenaddress.greenapi.HDKey;
import com.greenaddress.greenapi.HWWallet;
import com.greenaddress.greenapi.ISigningWallet;
import com.greenaddress.greenapi.Output;
import com.greenaddress.greenapi.PreparedTransaction;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.UnsafeByteArrayOutputStream;
import org.bitcoinj.core.VarInt;
import org.bitcoinj.crypto.DeterministicKey;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

public class BTChipHWWallet extends HWWallet {
    private static final ListeningExecutorService ES = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1));
    private final BTChipDongle mDongle;
    private final String mPin;
    private DeterministicKey mCachedPubkey;
    private final List<Integer> mAddrn;

    private static final String TAG = BTChipHWWallet.class.getSimpleName();

    private BTChipHWWallet(final BTChipDongle dongle, final String pin, final List<Integer> addrn) {
        this.mDongle = dongle;
        this.mPin = pin;
        this.mAddrn = addrn;
    }

    public BTChipHWWallet(final BTChipDongle dongle) {
        this(dongle, "0000", new LinkedList<Integer>());
    }

    public BTChipHWWallet(final BTChipTransport transport, final String pin) {
        this.mDongle = new BTChipDongle(transport);
        this.mPin = pin;
        this.mAddrn = new LinkedList<>();
    }

    public BTChipHWWallet(final BTChipTransport transport) {
        this(transport, null);
    }

    public BTChipHWWallet(final BTChipTransport transport, final String pin, final SettableFuture<Integer> remainingAttemptsFuture) {
        this(transport, pin);
        ES.submit(new Callable<Object>() {
            @Override
            public Object call() {
                try {
                    mDongle.verifyPin(BTChipHWWallet.this.mPin.getBytes());
                    remainingAttemptsFuture.set(-1);  // -1 means success
                } catch (final BTChipException e) {
                    e.printStackTrace();
                    if (e.toString().contains("63c"))
                        remainingAttemptsFuture.set(
                                Integer.valueOf(String.valueOf(e.toString().charAt(e.toString().indexOf("63c") + 3))));
                    else if (e.toString().contains("6985"))
                        // mDongle is not set up
                        remainingAttemptsFuture.set(0);
                    else
                        remainingAttemptsFuture.setException(e);
                } catch (final Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
    }

    private String outToPath(final Output out) {
        final String BRANCH = Integer.toString(HDKey.BRANCH_REGULAR) + "/";
        if (out.subAccount != 0)
            return "3'/" + out.subAccount + "'/" + BRANCH + out.pointer;
        return BRANCH + out.pointer;
    }

    private List<byte[]> signSegwitInputs(final PreparedTransaction ptx) throws BTChipException, IOException {
        final Transaction decoded = ptx.mDecoded;
        final List<TransactionInput> txInputs = decoded.getInputs();
        final List<Output> prevOuts = ptx.mPrevOutputs;
        final BTChipDongle.BTChipInput inputs[] = new BTChipDongle.BTChipInput[txInputs.size()];
        final List<byte[]> sigs = new LinkedList<>();

        for (int i = 0; i < txInputs.size(); ++i) {
            final TransactionInput txInput = txInputs.get(i);
            final TransactionOutPoint txOutpoint = txInput.getOutpoint();
            final byte[] inputHash = txOutpoint.getHash().getReversedBytes();
            final Output prevOut = prevOuts.get(i);
            ByteArrayOutputStream inputBuf = new ByteArrayOutputStream();
            inputBuf.write(inputHash, 0, inputHash.length);
            long index = txOutpoint.getIndex();
            BufferUtils.writeUint32LE(inputBuf, index);
            BufferUtils.writeUint64LE(inputBuf, prevOut.value);
            ByteArrayOutputStream sequenceBuf = new ByteArrayOutputStream();
            BufferUtils.writeUint32LE(sequenceBuf, txInput.getSequenceNumber());
            inputs[i] = mDongle.createInput(inputBuf.toByteArray(), sequenceBuf.toByteArray(), false, true);
        }

        // Prepare the pseudo transaction
        BTChipDongle.BTChipInput singleInput[] = new BTChipDongle.BTChipInput[1];
        final List<TransactionOutput> txOutputs = decoded.getOutputs();
        Output output = ptx.mPrevOutputs.get(0);
        // Provide the first script instead of a null script to initialize the P2SH confirmation logic
        mDongle.startUntrustedTransction(true, 0, inputs, Wally.hex_to_bytes(output.script), true);
        final int msgSize = decoded.getMessageSize();
        final ByteArrayOutputStream stream = new UnsafeByteArrayOutputStream(msgSize < 32 ? 32 : msgSize + 32);
        stream.write(new VarInt(txOutputs.size()).encode());
        for (final TransactionOutput out : txOutputs)
            out.bitcoinSerialize(stream);
        mDongle.finalizeInputFull(stream.toByteArray());
        // Sign each input
        for (int i = 0; i < txInputs.size(); ++i) {
            output = ptx.mPrevOutputs.get(i);
            singleInput[0] = inputs[i];
            final Output prevOut = prevOuts.get(i);
            if (prevOut.scriptType != 14) // sign segwit only
                continue;
            mDongle.startUntrustedTransction(false, 0, singleInput, Wally.hex_to_bytes(output.script), true);
            final ECKey.ECDSASignature sig;
            sig = ECKey.ECDSASignature.decodeFromDER(mDongle.untrustedHashSign(outToPath(output),
                    "0", decoded.getLockTime(), (byte) 1 /* = SIGHASH_ALL */));
            sigs.add(ISigningWallet.getTxSignature(sig));
        }

        return sigs;
    }

    private List<byte[]> signNonSegwitInputs(final PreparedTransaction ptx) throws BTChipException, IOException {
        final Transaction decoded = ptx.mDecoded;
        final List<TransactionInput> txInputs = decoded.getInputs();
        final List<Output> prevOuts = ptx.mPrevOutputs;
        final List<byte[]> sigs = new LinkedList<>();

        final BTChipDongle.BTChipInput inputs[] = new BTChipDongle.BTChipInput[txInputs.size()];
        if (!mDongle.understandsMultipleOutputs()) {
            for (int i = 0; i < txInputs.size(); ++i) {
                final TransactionInput txInput = txInputs.get(i);
                final TransactionOutPoint txOutpoint = txInput.getOutpoint();
                final byte[] inputHash = txOutpoint.getHash().getReversedBytes();
                ByteArrayOutputStream inputBuf = new ByteArrayOutputStream();
                inputBuf.write(inputHash, 0, inputHash.length);
                long index = txOutpoint.getIndex();
                BufferUtils.writeUint32LE(inputBuf, index);
                ByteArrayOutputStream sequenceBuf = new ByteArrayOutputStream();
                BufferUtils.writeUint32LE(sequenceBuf, txInput.getSequenceNumber());
                inputs[i] = mDongle.createInput(inputBuf.toByteArray(), sequenceBuf.toByteArray(), false, false);
            }
        } else {
            for (int i = 0; i < txInputs.size(); ++i) {
                final TransactionInput txInput = txInputs.get(i);
                final TransactionOutPoint txOutpoint = txInput.getOutpoint();
                final long index = txOutpoint.getIndex();
                final ByteArrayInputStream in = new ByteArrayInputStream(ptx.mPrevoutRawTxs.get(txOutpoint.getHash().toString()).unsafeBitcoinSerialize());
                final BitcoinTransaction encodedTx = new BitcoinTransaction(in);
                inputs[i] = mDongle.getTrustedInput(encodedTx, index, txInput.getSequenceNumber());
            }
        }
        for (int i = 0; i < txInputs.size(); ++i) {
            final List<TransactionOutput> txOutputs = decoded.getOutputs();
            final Output output = ptx.mPrevOutputs.get(i);
            mDongle.startUntrustedTransction(i == 0, i, inputs, Wally.hex_to_bytes(output.script), false);
            final int msgSize = decoded.getMessageSize();
            final ByteArrayOutputStream stream = new UnsafeByteArrayOutputStream(msgSize < 32 ? 32 : msgSize + 32);
            stream.write(new VarInt(txOutputs.size()).encode());
            for (final TransactionOutput out : txOutputs)
                out.bitcoinSerialize(stream);
            mDongle.finalizeInputFull(stream.toByteArray());
            final Output prevOut = prevOuts.get(i);
            if (prevOut.scriptType == 14) // don't sign segwit
                continue;
            final ECKey.ECDSASignature sig;
            sig = ECKey.ECDSASignature.decodeFromDER(mDongle.untrustedHashSign(outToPath(output),
                    "0", decoded.getLockTime(), (byte) 1 /* = SIGHASH_ALL */));
            sigs.add(ISigningWallet.getTxSignature(sig));
        }
        return sigs;
    }

    @Override
    public List<byte[]> signTransaction(final PreparedTransaction ptx) {
        final Transaction decoded = ptx.mDecoded;
        final List<TransactionInput> txInputs = decoded.getInputs();
        final List<Output> prevOuts = ptx.mPrevOutputs;

        final List<byte[]> sigs = new LinkedList<>();
        try {
            boolean segwit = false, nonSegwit = false;
            for (int i = 0; i < txInputs.size(); ++i) {
                final Output prevOut = prevOuts.get(i);
                if (prevOut.scriptType.equals(14)) {
                    segwit = true;
                } else {
                    nonSegwit = true;
                }
            }
            // Sanity check on the firmware version, in case devices have been swapped
            if (segwit && !mDongle.shouldUseNewSigningApi()) {
                throw new RuntimeException("Segwit not supported");
            }

            final List<byte[]> segwitSigs, nonSegwitSigs;

            if (segwit) {
                segwitSigs = signSegwitInputs(ptx);
            } else {
                segwitSigs = new LinkedList<>();
            }

            if (nonSegwit) {
                nonSegwitSigs = signNonSegwitInputs(ptx);
            } else {
                nonSegwitSigs = new LinkedList<>();
            }

            for (int i = 0; i < txInputs.size(); ++i) {
                final Output prevOut = prevOuts.get(i);
                if (prevOut.scriptType.equals(14)) {
                    sigs.add(segwitSigs.remove(0));
                } else {
                    sigs.add(nonSegwitSigs.remove(0));
                }
            }

            return sigs;
        } catch (final BTChipException | IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public DeterministicKey getPubKey() {
        try {
            return internalGetPubKey();
        } catch (final BTChipException e) {
            return null;
        }
    }

    private DeterministicKey internalGetPubKey() throws BTChipException {
        if (mCachedPubkey == null) {
            final BTChipDongle.BTChipPublicKey walletKey = mDongle.getWalletPublicKey(getPath());
            mCachedPubkey = HDKey.createMasterKey(walletKey.getChainCode(), walletKey.getPublicKey());
        }
        return mCachedPubkey;
    }

    @Override
    protected ECKey.ECDSASignature signMessage(final String message) {
        try {
            mDongle.signMessagePrepare(getPath(), message.getBytes());
            final BTChipDongle.BTChipSignature sig = mDongle.signMessageSign(new byte[]{0});
            return ECKey.ECDSASignature.decodeFromDER(sig.getSignature());
        } catch (final BTChipException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private String getPath() {
        final List<String> pathStr = new LinkedList<>();
        for (final Integer i : mAddrn) {
            String s = String.valueOf(i & ~0x80000000);
            if ((i & 0x80000000) != 0)
                s = s + "'";
            pathStr.add(s);
        }
        return Joiner.on("/").join(pathStr);
    }

    @Override
    protected HWWallet derive(final Integer childNumber) {
        final LinkedList<Integer> addrn_child = new LinkedList<>(mAddrn);
        addrn_child.add(childNumber);
        return new BTChipHWWallet(mDongle, mPin, addrn_child);
    }

    public BTChipDongle getDongle() {
        return mDongle;
    }

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
                mDongle.verifyPin(BTChipHWWallet.this.mPin.getBytes());
        }
        catch(final Exception e) {
        }
    }

    @Override
    public Object[] getChallengeArguments() {
        return getChallengeArguments(false);
    }
}
