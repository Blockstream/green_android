package com.greenaddress.greenapi;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.DeterministicKey;

import java.util.List;

public abstract class ISigningWallet {
    public static final int HARDENED = 0x80000000;

    public abstract ISigningWallet derive(Integer childNumber);

    public abstract byte[] getIdentifier();

    public abstract ECKey.ECDSASignature signHash(byte[] hash);

    public abstract ECKey.ECDSASignature signMessage(String message);

    public abstract boolean canSignHashes();

    public abstract DeterministicKey getPubKey();

    public abstract List<ECKey.ECDSASignature> signTransaction(PreparedTransaction ptx);

    public abstract boolean requiresPrevoutRawTxs();
}
