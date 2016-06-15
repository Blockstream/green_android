package com.greenaddress.greenapi;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.DeterministicKey;

import java.util.List;

public abstract class ISigningWallet {
    protected static final int HARDENED = 0x80000000;

    public abstract boolean requiresPrevoutRawTxs(); // FIXME: Get rid of this

    public abstract DeterministicKey getMyPublicKey(final int subAccount, final Integer pointer);
    public abstract List<ECKey.ECDSASignature> signTransaction(PreparedTransaction ptx);
    // FIXME: This is only needed until the challenge RPC is unified
    public abstract Object[] getChallengeArguments();
    public abstract String[] signChallenge(final String challengeString, final String[] challengePath);
}
