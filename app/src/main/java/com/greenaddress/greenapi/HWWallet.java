package com.greenaddress.greenapi;

import com.blockstream.libwally.Wally;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.crypto.DeterministicKey;

public abstract class HWWallet extends ISigningWallet {

    @Override
    public boolean requiresPrevoutRawTxs() { return true; }

    @Override
    public DeterministicKey getMyPublicKey(final int subAccount, final Integer pointer) {
        DeterministicKey k = getMyKey(subAccount).getPubKey();
        // Currently only regular transactions are supported
        k = HDKey.deriveChildKey(k, HDKey.BRANCH_REGULAR);
        return HDKey.deriveChildKey(k, pointer);
    }

    @Override
    public String[] signChallenge(final String challengeString, final String[] challengePath) {

        // Generate a path for the challenge.
        // We use "GA" + 0xB11E as the child path as this allows btchip to skip HID auth.
        final HWWallet child = this.derive(0x4741b11e); // 0x4741 = Ascii G << 8 + A

        // Generate a message to sign from the challenge
        final String challenge = "greenaddress.it      login " + challengeString;
        final byte[] rawHash = Wally.format_bitcoin_message(challenge.getBytes(),
                                                            Wally.BITCOIN_MESSAGE_FLAG_HASH);
        final Sha256Hash hash = Sha256Hash.wrap(rawHash);

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

    private HWWallet getMyKey(final int subAccount) {
        HWWallet parent = this;
        if (subAccount != 0)
            parent = parent.derive(ISigningWallet.HARDENED | 3)
                           .derive(ISigningWallet.HARDENED | subAccount);
        return parent;
    }

    protected Object[] getChallengeArguments(final boolean isTrezor) {
        final byte[] id = getPubKey().toAddress(Network.NETWORK).getHash160();
        final Address addr = new Address(Network.NETWORK, id);
        return new Object[]{ "login.get_trezor_challenge", addr.toString(), !isTrezor };
    }

    protected abstract DeterministicKey getPubKey();
    protected abstract HWWallet derive(Integer childNumber);
    protected abstract ECKey.ECDSASignature signMessage(String message);
}
