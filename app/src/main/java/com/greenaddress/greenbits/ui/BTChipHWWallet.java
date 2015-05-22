package com.greenaddress.greenbits.ui;


import com.btchip.BTChipDongle;
import com.btchip.BTChipException;
import com.btchip.comm.BTChipTransport;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.greenaddress.greenapi.ISigningWallet;
import com.greenaddress.greenapi.Output;
import com.greenaddress.greenapi.PreparedTransaction;

import org.bitcoinj.core.*;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.spongycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;


public class BTChipHWWallet implements ISigningWallet {
    private static final ListeningExecutorService es = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1));
    private BTChipDongle dongle;
    private RequestLoginActivity loginActivity;
    private String pin;
    private List<Integer> addrn = new LinkedList<>();

    private BTChipHWWallet(BTChipDongle dongle, RequestLoginActivity loginActivity, String pin, List<Integer> addrn) {
        this.dongle = dongle;
        this.loginActivity = loginActivity;
        this.pin = pin;
        this.addrn = addrn;
    }

    public BTChipHWWallet(BTChipTransport transport, RequestLoginActivity loginActivity, String pin, final SettableFuture<Integer> remainingAttemptsFuture) {
        this.dongle = new BTChipDongle(transport);
        this.loginActivity = loginActivity;
        this.pin = pin;
        es.submit(new Callable<Object>() {
            @Override
            public Object call() {
                try {
                    dongle.verifyPin(BTChipHWWallet.this.pin.getBytes());
                    remainingAttemptsFuture.set(new Integer(-1));  // -1 means success
                } catch (BTChipException e) {
                    if (e.toString().indexOf("63c") != -1) {
                        remainingAttemptsFuture.set(
                                Integer.valueOf(String.valueOf(e.toString().charAt(e.toString().indexOf("63c") + 3))));
                    } else if (e.toString().indexOf("6985") != -1) {
                        // dongle is not set up
                        remainingAttemptsFuture.set(0);
                    } else {
                        remainingAttemptsFuture.setException(e);
                    }
                    e.printStackTrace();
                }
                return null;
            }
        });
    }

    private String outToPath(Output out) {
        if (out.getSubaccount() != null && out.getSubaccount().intValue() != 0) {
            return "3'/" + out.getSubaccount() + "'/1/" + out.getPointer();
        } else {
            return "1/" + out.getPointer();
        }
    }

    @Override
    public ListenableFuture<List<ECKey.ECDSASignature>> signTransaction(final PreparedTransaction tx, String coinName, byte[] gait_path) {
        return es.submit(new Callable<List<ECKey.ECDSASignature>>() {
            @Override
            public List<ECKey.ECDSASignature> call() throws Exception {
                List<ECKey.ECDSASignature> sigs = new LinkedList<>();

                BTChipDongle.BTChipInput inputs[] = new BTChipDongle.BTChipInput[tx.decoded.getInputs().size()];
                for (int i = 0; i < tx.decoded.getInputs().size(); ++i) {
                    byte[] inputHash = tx.decoded.getInputs().get(i).getOutpoint().getHash().getBytes();
                    for (int j = 0; j < inputHash.length / 2; ++j) {
                        byte temp = inputHash[j];
                        inputHash[j] = inputHash[inputHash.length - j - 1];
                        inputHash[inputHash.length - j - 1] = temp;
                    }
                    byte[] input = Arrays.copyOf(inputHash, inputHash.length + 4);
                    long index = tx.decoded.getInputs().get(i).getOutpoint().getIndex();
                    input[input.length - 4] = (byte)(index % 256); index /= 256;
                    input[input.length - 3] = (byte)(index % 256); index /= 256;
                    input[input.length - 2] = (byte)(index % 256); index /= 256;
                    input[input.length - 1] = (byte)(index % 256);
                    inputs[i] = dongle.createInput(input, false);
                }
                for (int i = 0; i < tx.decoded.getInputs().size(); ++i) {
                    dongle.startUntrustedTransction(i == 0, i, inputs, Hex.decode(tx.prev_outputs.get(i).getScript()));
                    ByteArrayOutputStream stream = new UnsafeByteArrayOutputStream(tx.decoded.getMessageSize() < 32 ? 32 : tx.decoded.getMessageSize() + 32);
                    stream.write(new VarInt(tx.decoded.getOutputs().size()).encode());
                    for (TransactionOutput out : tx.decoded.getOutputs())
                        out.bitcoinSerialize(stream);
                    dongle.finalizeInputFull(stream.toByteArray());
                    sigs.add(ECKey.ECDSASignature.decodeFromDER(
                            dongle.untrustedHashSign(outToPath(tx.prev_outputs.get(i)),
                                "0", tx.decoded.getLockTime(), (byte)1 /* = SIGHASH_ALL */)));
                }

                return sigs;
            }
        });
    }



    @Override
    public ListenableFuture<DeterministicKey> getPubKey() {
        return es.submit(new Callable<DeterministicKey>() {
            @Override
            public DeterministicKey call() throws Exception {
                BTChipDongle.BTChipPublicKey pubKey = dongle.getWalletPublicKey(getPath());
                ECKey uncompressed = ECKey.fromPublicOnly(pubKey.getPublicKey());
                DeterministicKey retVal = new DeterministicKey(
                        new ImmutableList.Builder<ChildNumber>().build(),
                        pubKey.getChainCode(),
                        uncompressed.getPubKeyPoint(),
                        null, null
                );
                return retVal;
            }
        });
    }

    @Override
    public boolean canSignHashes() {
        return false;
    }

    @Override
    public ListenableFuture<ECKey.ECDSASignature> signMessage(final String message) {
        return es.submit(new Callable<ECKey.ECDSASignature>() {
            @Override
            public ECKey.ECDSASignature call() throws Exception {
                dongle.signMessagePrepare(getPath(), message.getBytes());
                BTChipDongle.BTChipSignature sig = dongle.signMessageSign(new byte[] { 0 });
                return ECKey.ECDSASignature.decodeFromDER(sig.getSignature());
            }
        });
    }

    @Override
    public ListenableFuture<ECKey.ECDSASignature> signHash(Sha256Hash hash) {
        return null;
    }

    @Override
    public ListenableFuture<byte[]> getIdentifier() {
        return Futures.transform(getPubKey(), new Function<ECKey, byte[]>() {
            @Nullable
            @Override
            public byte[] apply(@Nullable ECKey input) {
                return input.toAddress(NetworkParameters.fromID(NetworkParameters.ID_MAINNET)).getHash160();
            }
        });
    }

    private String getPath() {
        List<String> pathStr = new LinkedList<>();
        for (Integer i : addrn) {
            String s = String.valueOf(i.intValue() & ~0x80000000);
            if ((i.intValue() & 0x80000000) != 0) {
                s = s + "'";
            }
            pathStr.add(s);
        }
        return Joiner.on("/").join(pathStr);
    }

    @Override
    public ISigningWallet deriveChildKey(ChildNumber childNumber) {
        LinkedList<Integer> addrn_child = new LinkedList<>(addrn);
        addrn_child.add(childNumber.getI());
        return new BTChipHWWallet(dongle, loginActivity, pin, addrn_child);
    }
}
