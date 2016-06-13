package com.greenaddress.greenapi;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.crypto.DeterministicKey;

import java.util.List;

public class Bip32Wallet extends ISigningWallet {

    public Bip32Wallet(final DeterministicKey key) {
        super(key);
    }

    @Override
    public ISigningWallet derive(final Integer childNumber) {
        return new Bip32Wallet(HDKey.deriveChildKey(mRootKey, childNumber));
    }

    @Override
    public byte[] getIdentifier() {
        return mRootKey.getIdentifier();
    }

    @Override
    public ECKey.ECDSASignature signHash(final byte[] hash) {
        return ECKey.fromPrivate(mRootKey.getPrivKey()).sign(Sha256Hash.wrap(hash));
    }

    @Override
    public ECKey.ECDSASignature signMessage(final String message) {
        return null;
    }

    @Override
    public DeterministicKey getPubKey() {
        return mRootKey;
    }

    @Override
    public boolean requiresPrevoutRawTxs() { return false; }
}
