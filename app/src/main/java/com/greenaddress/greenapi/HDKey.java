package com.greenaddress.greenapi;

import com.blockstream.libwally.Wally;

import com.google.common.collect.ImmutableList;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;

import static com.blockstream.libwally.Wally.BIP32_VER_MAIN_PUBLIC;
import static com.blockstream.libwally.Wally.BIP32_VER_TEST_PUBLIC;
import static com.blockstream.libwally.Wally.BIP32_VER_MAIN_PRIVATE;
import static com.blockstream.libwally.Wally.BIP32_VER_TEST_PRIVATE;

public class HDKey {
    private final static int VER_PUBLIC = isMain() ? BIP32_VER_MAIN_PUBLIC : BIP32_VER_TEST_PUBLIC;
    private final static int VER_PRIVATE = isMain() ? BIP32_VER_MAIN_PRIVATE : BIP32_VER_TEST_PRIVATE;

    private Object mImpl;

    private static boolean isMain() {
        return NetworkParameters.fromID(NetworkParameters.ID_MAINNET).equals(Network.NETWORK);
    }

    public HDKey(final byte[] seed) {
        mImpl = Wally.bip32_key_from_seed(seed, VER_PRIVATE);
    }

    public HDKey(final byte[] chainCode, final byte[] publicKey) {
        mImpl = Wally.bip32_key_init(VER_PUBLIC, 0, 0, chainCode, publicKey, null, null, null);
    }

    public void close() {
        if (mImpl == null)
            return;
        Wally.bip32_key_free(mImpl);
        mImpl = null;
    }

    public final byte[] getPrivateKey() { return Wally.bip32_key_get_priv_key(mImpl); }
    public final byte[] getPublicKey() { return Wally.bip32_key_get_pub_key(mImpl); }
    public final byte[] getChainCode() { return Wally.bip32_key_get_chain_code(mImpl); }
    public final int getChildNumber() { return Wally.bip32_key_get_child_num(mImpl); }

    public HDKey derive(final Integer childNum) {
        return derive(childNum, getPrivateKey() != null ? VER_PRIVATE : VER_PUBLIC);
    }
    public HDKey derivePublic(final Integer childNum) { return derive(childNum, VER_PUBLIC); }
    public HDKey derivePrivate(final Integer childNum) { return derive(childNum, VER_PRIVATE); }

    private HDKey derive(final Integer childNum, int version) {
        Object derived = Wally.bip32_key_from_parent(mImpl, childNum, version);
        close();
        mImpl = derived;
        return this;
    }

    private static byte[] h(final String hex) { return Wally.hex_to_bytes(hex); }

    //
    // Temporary methods for use while converting from DeterministicKey
    public static DeterministicKey deriveChildKey(final DeterministicKey parent, final Integer childNum) {
        return HDKeyDerivation.deriveChildKey(parent, new ChildNumber(childNum));
    }

    public static DeterministicKey createMasterKeyFromSeed(final byte[] seed) {
        return HDKeyDerivation.createMasterPrivateKey(seed);
    }

    public static DeterministicKey createMasterKey(final byte[] chainCode, final byte[] publicKey) {
        final ECKey pub = ECKey.fromPublicOnly(publicKey);
        return new DeterministicKey(new ImmutableList.Builder<ChildNumber>().build(),
                                    chainCode, pub.getPubKeyPoint(), null, null);
    }

    public static DeterministicKey createMasterKey(final String chainCode, final String publicKey) {
        return createMasterKey(h(chainCode), h(publicKey));
    }

    // Get the key derived from the servers public key/chaincode plus the users path.
    // This is the key used on the servers side of 2-of-2 transactions.
    public static DeterministicKey getServerSubaccountKey(final int[] path, final Integer subaccount) {
        DeterministicKey k = createMasterKey(Network.depositChainCode, Network.depositPubkey);
        k = deriveChildKey(k, subaccount == 0 ? 1 : 3);
        for (int i : path)
            k = deriveChildKey(k, i);
        if (subaccount != 0)
            k = deriveChildKey(k, subaccount);

        // Reconcile against wally
        HDKey hd = new HDKey(h(Network.depositChainCode), h(Network.depositPubkey));
        hd.derivePublic(subaccount == 0 ? 1 : 3);
        for (int i : path)
            hd.derivePublic(i);
        if (subaccount != 0)
            hd.derivePublic(subaccount);

        if (!h(k.getChainCode()).equals(h(hd.getChainCode())))
            throw new RuntimeException("Chain code mismatch");
        if (!h(k.getPubKey()).equals(h(hd.getPublicKey())))
            throw new RuntimeException("Public key mismatch");

        return k;
    }
    // FIXME: Remove
    private static String h(final byte[] bytes) { return Wally.hex_from_bytes(bytes); }

}
