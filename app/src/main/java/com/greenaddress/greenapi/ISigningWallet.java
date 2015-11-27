package com.greenaddress.greenapi;

import com.google.common.util.concurrent.ListenableFuture;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;

import java.util.List;

public interface ISigningWallet {
    ISigningWallet deriveChildKey(ChildNumber childNumber);

    ListenableFuture<byte[]> getIdentifier();

    ListenableFuture<ECKey.ECDSASignature> signHash(Sha256Hash hash);

    ListenableFuture<ECKey.ECDSASignature> signMessage(String message);

    boolean canSignHashes();

    ListenableFuture<DeterministicKey> getPubKey();

    ListenableFuture<List<ECKey.ECDSASignature>> signTransaction(PreparedTransaction tx, byte[] gait_path);

    boolean requiresPrevoutRawTxs();
}
