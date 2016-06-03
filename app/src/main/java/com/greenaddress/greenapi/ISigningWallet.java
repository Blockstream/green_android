package com.greenaddress.greenapi;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.DeterministicKey;

import java.util.List;

public interface ISigningWallet {
    public static final int HARDENED = 0x80000000;

    ISigningWallet derive(Integer childNumber);

    byte[] getIdentifier();

    ECKey.ECDSASignature signHash(byte[] hash);

    ECKey.ECDSASignature signMessage(String message);

    boolean canSignHashes();

    DeterministicKey getPubKey();

    List<ECKey.ECDSASignature> signTransaction(PreparedTransaction ptx, byte[] gaitPath);

    boolean requiresPrevoutRawTxs();
}
