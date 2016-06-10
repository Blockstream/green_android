package com.greenaddress.greenapi;

import com.blockstream.libwally.Wally;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Utils;
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

    public String[] signChallenge(final String challengeString, final String[] challengePath) {

        // Generate a path for the challenge. This is really a nonce so we aren't
        // tricked into signing the same challenge (and thus revealing our key)
        // by a compromised server.
        final byte[] path = CryptoHelper.randomBytes(8);

        // Return the path to the caller for them to pass in the server RPC call
        challengePath[0] = Wally.hex_from_bytes(path);

        // Derive the private key for signing the challenge from the path
        DeterministicKey key = mRootKey;
        for (int i = 0; i < path.length; ++i) {
            int step = u8(path[i * 2]) * 256 + u8(path[i * 2 + 1]);
            key = HDKey.deriveChildKey(key, step);
        }

        // Get rid of initial 0 byte if challenge > 2^31
        // FIXME: The server should not send us challenges that we have to munge!
        byte[] challenge = new BigInteger(challengeString).toByteArray();
        if (challenge.length == 33 && challenge[0] == 0)
            challenge = Arrays.copyOfRange(challenge, 1, 33);

        // Compute and return the challenge signatures
        final ECKey.ECDSASignature signature;
        signature = ECKey.fromPrivate(key.getPrivKey()).sign(Sha256Hash.wrap(challenge));
        return new String[]{signature.r.toString(), signature.s.toString()};
    }

    protected String[] signChallengeHW(final String challengeString, final String[] challengePath) {

        // Generate a path for the challenge.
        // We use "GA" + 0xB11E as the child path as this allows btchip to skip HID auth.
        final ISigningWallet child = this.derive(0x4741b11e); // 0x4741 = Ascii G << 8 + A

        // Generate a message to sign from the challenge
        final String challenge = "greenaddress.it      login " + challengeString;
        final Sha256Hash hash = Sha256Hash.wrap(Wally.sha256d(Utils.formatMessageForSigning(challenge)));

        // Return the path to the caller for them to pass in the server RPC call
        challengePath[0] = "GA";

        // Compute and return the challenge signatures
        final ECKey.ECDSASignature signature = child.signMessage(challenge);
        int recId;
        for (recId = 0; recId < 4; ++recId) {
            final ECKey recovered = ECKey.recoverFromSignature(recId, signature, hash, true);
            if (recovered != null && recovered.equals(child.getPubKey()))
                break;
        }
        return new String[]{signature.r.toString(), signature.s.toString(), String.valueOf(recId)};
    }

    private int u8(int i) { return i < 0 ? 256 + i : i; }
}
