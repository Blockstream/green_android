package com.greenaddress.greenapi;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.TransactionSignature;

import java.util.List;

public abstract class ISigningWallet {
    protected static final int HARDENED = 0x80000000;

    public abstract boolean requiresPrevoutRawTxs(); // FIXME: Get rid of this

    public abstract DeterministicKey getMyPublicKey(final int subAccount, final Integer pointer);
    public abstract List<byte[]> signTransaction(PreparedTransaction ptx);
    public abstract List<byte[]> signTransaction(final Transaction tx, final List<Output> prevOuts);

    // FIXME: This is only needed until the challenge RPC is unified
    public abstract Object[] getChallengeArguments();
    public abstract String[] signChallenge(final String challengeString, final String[] challengePath);

    public static byte[] getTxSignature(final ECKey.ECDSASignature sig) {
        final TransactionSignature txSig = new TransactionSignature(sig, Transaction.SigHash.ALL, false);
        return txSig.encodeToBitcoin();
    }
}
