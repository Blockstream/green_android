package com.greenaddress.greenapi;

import com.blockstream.libwally.Wally;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.script.Script;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SWWallet extends ISigningWallet {

    private final DeterministicKey mRootKey;

    public SWWallet(final String mnemonic) {
        final byte[] seed = CryptoHelper.mnemonic_to_seed(mnemonic);
        mRootKey = HDKey.createMasterKeyFromSeed(seed);
    }

    public SWWallet(final DeterministicKey key) {
        mRootKey = key;
    }

    private SWWallet derive(final Integer childNumber) {
        return new SWWallet(HDKey.deriveChildKey(mRootKey, childNumber));
    }

    @Override
    public DeterministicKey getSubAccountPublicKey(final int subAccount) {
        return getMyKey(subAccount).mRootKey;
    }

    @Override
    public List<byte[]> signTransaction(final PreparedTransaction ptx) {
        return signTransaction(ptx.mDecoded, ptx, ptx.mPrevOutputs);
    }

    @Override
    public List<byte[]> signTransaction(final Transaction tx, final PreparedTransaction ptx, final List<Output> prevOuts) {
        final List<TransactionInput> txInputs = tx.getInputs();
        final List<byte[]> sigs = new ArrayList<>(txInputs.size());

        for (int i = 0; i < txInputs.size(); ++i) {
            final Output prevOut = prevOuts.get(i);

            final Script script = new Script(Wally.hex_to_bytes(prevOut.script));
            final Sha256Hash hash;
            if (prevOut.scriptType.equals(GATx.P2SH_P2WSH_FORTIFIED_OUT))
                hash = tx.hashForSignatureWitness(i, script.getProgram(), Coin.valueOf(prevOut.value), Transaction.SigHash.ALL, false);
            else
                hash = tx.hashForSignature(i, script.getProgram(), Transaction.SigHash.ALL, false);

            final SWWallet key = getMyKey(prevOut.subAccount).derive(prevOut.branch).derive(prevOut.pointer);
            final ECKey eckey = ECKey.fromPrivate(key.mRootKey.getPrivKey());
            sigs.add(getTxSignature(eckey.sign(Sha256Hash.wrap(hash.getBytes()))));
        }
        return sigs;
    }

    @Override
    public Object[] getChallengeArguments() {
        final Address addr = new Address(Network.NETWORK, mRootKey.getIdentifier());
        return new Object[]{ "login.get_challenge", addr.toString() };
    }

    @Override
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
            final int step = u8(path[i * 2]) * 256 + u8(path[i * 2 + 1]);
            key = HDKey.deriveChildKey(key, step);
        }

        // Get rid of initial 0 byte if challenge > 2^31
        // FIXME: The server should not send us challenges that we have to munge!
        byte[] challenge = new BigInteger(challengeString).toByteArray();
        if (challenge.length == 33 && challenge[0] == 0)
            challenge = Arrays.copyOfRange(challenge, 1, 33);

        // Compute and return the challenge signatures
        final ECKey.ECDSASignature sig;
        sig = ECKey.fromPrivate(key.getPrivKey()).sign(Sha256Hash.wrap(challenge));
        return new String[]{ sig.r.toString(), sig.s.toString() };
    }

    public DeterministicKey getMasterKey() {
        return mRootKey;
    }

    private SWWallet getMyKey(final int subAccount) {
        if (subAccount != 0)
            return derive(HARDENED | 3).derive(HARDENED | subAccount);
        return this;
    }

    public byte[] getLocalEncryptionPassword() {
        final byte[] pubkey = derive(PASSWORD_PATH).mRootKey.getPubKey();
        return CryptoHelper.pbkdf2_hmac_sha512(pubkey, PASSWORD_SALT);
    }

    private int u8(final int i) { return i < 0 ? 256 + i : i; }

    public byte[] getGAPath() {
        final DeterministicKey key = derive(GA_PATH).mRootKey;
        return extendedKeyToPath(key.getPubKey(), key.getChainCode());
    }
}
