package com.greenaddress.greenbits.wallets;

import com.greenaddress.greenapi.HDKey;
import com.greenaddress.greenapi.ISigningWallet;
import com.greenaddress.greenapi.Network;
import com.greenaddress.greenapi.PreparedTransaction;
import com.satoshilabs.trezor.Trezor;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.params.MainNetParams;

import java.util.LinkedList;
import java.util.List;


public class TrezorHWWallet implements ISigningWallet {

    private final Trezor trezor;
    private final List<Integer> addrn;

    public TrezorHWWallet(final Trezor t) {
        trezor = t;
        addrn = new LinkedList<>();
    }

    private TrezorHWWallet(final TrezorHWWallet parent, final Integer childNumber) {
        trezor = parent.trezor;
        addrn = new LinkedList<>(parent.addrn);
        addrn.add(childNumber);
    }

    @Override
    public ISigningWallet deriveChildKey(final ChildNumber childNumber) {
        return new TrezorHWWallet(this, childNumber.getI());
    }

    @Override
    public byte[] getIdentifier() {
        return getPubKey().toAddress(Network.NETWORK).getHash160();
    }

    @Override
    public boolean canSignHashes() {
        return false;
    }

    @Override
    public ECKey.ECDSASignature signHash(final byte[] hash) {
        return null;
    }

    @Override
    public ECKey.ECDSASignature signMessage(final String message) {
        final Integer[] intArray = new Integer[addrn.size()];
        return trezor.MessageSignMessage(addrn.toArray(intArray), message);
    }

    @Override
    public DeterministicKey getPubKey() {
        final Integer[] intArray = new Integer[addrn.size()];
        final String[] xpub = trezor.MessageGetPublicKey(addrn.toArray(intArray)).split("%", -1);
        return HDKey.createMasterKey(xpub[xpub.length - 4], xpub[xpub.length - 2]);
    }

    @Override
    public List<ECKey.ECDSASignature> signTransaction(final PreparedTransaction tx, final byte[] gaitPath) {
        final boolean isMainnet = Network.NETWORK.getId().equals(MainNetParams.ID_MAINNET);
        return trezor.MessageSignTx(tx, isMainnet ? "Bitcoin": "Testnet", gaitPath);
    }

    @Override
    public boolean requiresPrevoutRawTxs() {
        return true;
    }
}
