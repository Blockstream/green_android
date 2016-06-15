package com.greenaddress.greenapi;

import com.blockstream.libwally.Wally;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.Utils;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.script.Script;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public abstract class ISigningWallet {
    public static final int HARDENED = 0x80000000;

    protected final DeterministicKey mRootKey;

    public ISigningWallet(final DeterministicKey key) {
        mRootKey = key;
    }

    public abstract ISigningWallet derive(Integer childNumber);

    public abstract byte[] getIdentifier();

    public abstract ECKey.ECDSASignature signMessage(String message);

    // FIXME: Get rid of these along with checking the object type by callers
    public boolean canSignHashes() { return true; }
    public boolean requiresPrevoutRawTxs() { return false; }

    public abstract DeterministicKey getPubKey();

    public List<ECKey.ECDSASignature> signTransaction(PreparedTransaction ptx) {
        final Transaction tx = ptx.decoded;
        final List<TransactionInput> txInputs = tx.getInputs();
        final List<Output> prevOuts = ptx.prev_outputs;
        final List<ECKey.ECDSASignature> sigs = new ArrayList<>(txInputs.size());

        for (int i = 0; i < txInputs.size(); ++i) {
            final Output prevOut = prevOuts.get(i);

            final Script script = new Script(Wally.hex_to_bytes(prevOut.script));
            final Sha256Hash hash;
            if (prevOut.scriptType.equals(14))
                hash = tx.hashForSignatureV2(i, script.getProgram(), Coin.valueOf(prevOut.value), Transaction.SigHash.ALL, false);
            else
                hash = tx.hashForSignature(i, script.getProgram(), Transaction.SigHash.ALL, false);

            final ISigningWallet key = getMyKey(prevOut.subAccount).derive(prevOut.branch).derive(prevOut.pointer);
            sigs.add(ECKey.fromPrivate(key.mRootKey.getPrivKey()).sign(Sha256Hash.wrap(hash.getBytes())));
        }
        return sigs;
    }

    private ISigningWallet getMyKey(final int subAccount) {
        ISigningWallet parent = this;
        if (subAccount != 0)
            parent = parent.derive(ISigningWallet.HARDENED | 3)
                           .derive(ISigningWallet.HARDENED | subAccount);
        return parent;
    }

    public DeterministicKey getMyPublicKey(final int subAccount, final Integer pointer) {
        DeterministicKey k = getMyKey(subAccount).getPubKey();
        // Currently only regular transactions are supported
        k = HDKey.deriveChildKey(k, HDKey.BRANCH_REGULAR);
        return HDKey.deriveChildKey(k, pointer);
    }

    public String[] signChallenge(final String challengeString, final String[] challengePath) {

        // Generate a path for the challenge. This is really a nonce so we aren't
        // tricked into signing the same challenge (and thus revealing our key)
        // by a compromised server.
        final byte[] path = CryptoHelper.randomBytes(8);

        // Return the path to the caller for them to pass in the server RPC call
        challengePath[0] = Wally.hex_from_bytes(path);

        // Derive the private key for signing the challenge from the path
        DeterministicKey key = mRootKey;
        for (int i = 0; i < path.length / 2; ++i) {
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
