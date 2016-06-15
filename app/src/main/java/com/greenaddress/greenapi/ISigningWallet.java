package com.greenaddress.greenapi;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.DeterministicKey;

import java.util.List;

public abstract class ISigningWallet {
    protected static final int HARDENED = 0x80000000;

    // FIXME: Get rid of these along with checking the object type by callers
    public abstract byte[] getIdentifier();
    public boolean canSignHashes() { return true; }
    public boolean requiresPrevoutRawTxs() { return false; }

    public abstract DeterministicKey getMyPublicKey(final int subAccount, final Integer pointer);
    public abstract List<ECKey.ECDSASignature> signTransaction(PreparedTransaction ptx);
    public abstract String[] signChallenge(final String challengeString, final String[] challengePath);
}
