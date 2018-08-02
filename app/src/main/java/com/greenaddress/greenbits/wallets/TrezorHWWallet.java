package com.greenaddress.greenbits.wallets;

import com.greenaddress.greenapi.HDKey;
import com.greenaddress.greenapi.HWWallet;
import com.greenaddress.greenapi.Network;
import com.greenaddress.greenapi.Output;
import com.greenaddress.greenapi.PreparedTransaction;
import com.satoshilabs.trezor.Trezor;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.DeterministicKey;

import android.util.Pair;

import java.util.LinkedList;
import java.util.List;


public class TrezorHWWallet extends HWWallet {

    private final Trezor trezor;
    private final List<Integer> addrn;

    public TrezorHWWallet(final Trezor t, final Network network) {
        trezor = t;
        addrn = new LinkedList<>();
        mNetwork = network;
    }

    private TrezorHWWallet(final TrezorHWWallet parent, final Integer childNumber, final Network network) {
        trezor = parent.trezor;
        addrn = new LinkedList<>(parent.addrn);
        addrn.add(childNumber);
        mNetwork = network;
    }

    @Override
    protected HWWallet derive(final Integer childNumber) {
        return new TrezorHWWallet(this, childNumber, mNetwork);
    }

    @Override
    protected ECKey.ECDSASignature signMessage(final String message) {
        return trezor.signMessage(addrn, message);
    }

    @Override
    public DeterministicKey getPubKey() {
        final Pair<byte[], byte[]> xpub = trezor.getUserKey(addrn);
        return HDKey.createMasterKey(xpub.second, xpub.first);
    }

    @Override
    public List<byte[]> signTransaction(final PreparedTransaction ptx) {
        return trezor.signTransaction(ptx, mNetwork.isMainnet() ? "Bitcoin" : "Testnet");
    }

    @Override
    public List<byte[]> signTransaction(final Transaction tx, final PreparedTransaction ptx, final List<Output> prevOuts) {
        ptx.mDecoded = tx;  // TODO: remove from PreparedTransaction
        ptx.mPrevOutputs = prevOuts;  // TODO: remove this argument since it's part of ptx
        return signTransaction(ptx);
    }

    @Override
    public Object[] getChallengeArguments() {
        return getChallengeArguments(true, mNetwork.getNetworkParameters());
    }
}
