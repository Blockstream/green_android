package com.greenaddress.greenapi;

import com.google.common.util.concurrent.ListenableFuture;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;

import java.util.List;

public interface ISigningWallet {
    ISigningWallet deriveChildKey(ChildNumber childNumber);

    byte[] getIdentifier();

    ListenableFuture<ECKey.ECDSASignature> signHash(byte[] hash);

    ListenableFuture<ECKey.ECDSASignature> signMessage(String message);

    boolean canSignHashes();

    DeterministicKey getPubKey();

    List<ECKey.ECDSASignature> signTransaction(PreparedTransaction tx, byte[] gait_path);

    boolean requiresPrevoutRawTxs();
}
