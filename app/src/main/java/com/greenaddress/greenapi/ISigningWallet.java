package com.greenaddress.greenapi;

import com.blockstream.libwally.Wally;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.DeterministicKey;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

public abstract class ISigningWallet {
    public static final int HARDENED = 0x80000000;

    protected final DeterministicKey mRootKey;

    public ISigningWallet(final DeterministicKey key) {
        mRootKey = key;
    }

    public abstract ISigningWallet derive(Integer childNumber);

    public abstract byte[] getIdentifier();

    public abstract ECKey.ECDSASignature signHash(byte[] hash);

    public abstract ECKey.ECDSASignature signMessage(String message);

    // FIXME: Get rid of these along with checking the object type by callers
    public boolean canSignHashes() { return true; }
    public boolean requiresPrevoutRawTxs() { return false; }

    public abstract DeterministicKey getPubKey();

    public abstract List<ECKey.ECDSASignature> signTransaction(PreparedTransaction ptx);

    String[] signChallenge(final String challengeString, final String[] challengePath) {

        // Generate a random challenge
        final byte[] path = CryptoHelper.randomBytes(8);

        // Return it to the caller for them to pass in the server RPC call
        challengePath[0] = Wally.hex_from_bytes(path);

        // Derive private key for signing the challenge
        DeterministicKey key = mRootKey;
        for (int i = 0; i < path.length; ++i) {
            int step = u8(path[i * 2]) * 256 + u8(path[i * 2 + 1]);
            key = HDKey.deriveChildKey(key, step);
        }

        // Get rid of initial 0 byte if challenge > 2^31
        byte[] challenge = new BigInteger(challengeString).toByteArray();
        if (challenge.length == 33 && challenge[0] == 0)
            challenge = Arrays.copyOfRange(challenge, 1, 33);

        ECKey.ECDSASignature sig = signHash(challenge);
        return new String[]{sig.r.toString(), sig.s.toString()};
    }

    private int u8(int i) { return i < 0 ? 256 + i : i; }
}
