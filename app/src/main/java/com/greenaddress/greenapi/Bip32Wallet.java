package com.greenaddress.greenapi;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;

import java.util.List;

public class Bip32Wallet implements ISigningWallet {
    private final DeterministicKey hdWallet;

    public Bip32Wallet(final DeterministicKey masterPrivateKey) {
        hdWallet = masterPrivateKey;
    }

    @Override
    public ISigningWallet deriveChildKey(final ChildNumber childNumber) {
        return new Bip32Wallet(HDKey.deriveChildKey(hdWallet, childNumber.getI()));
    }

    @Override
    public byte[] getIdentifier() {
        return hdWallet.getIdentifier();
    }

    @Override
    public ECKey.ECDSASignature signHash(final byte[] hash) {
        return ECKey.fromPrivate(hdWallet.getPrivKey()).sign(Sha256Hash.wrap(hash));
    }

    @Override
    public ECKey.ECDSASignature signMessage(final String message) {
        return null;
    }

    @Override
    public boolean canSignHashes() {
        return true;
    }

    @Override
    public DeterministicKey getPubKey() {
        return hdWallet;
    }

    @Override
    public List<ECKey.ECDSASignature> signTransaction(final PreparedTransaction tx, final byte[] gaitPath) {
        return null;
    }

    @Override
    public boolean requiresPrevoutRawTxs() {
        return false;
    }
}
