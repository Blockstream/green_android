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

    private final Object mImpl;

    static private boolean isMain() {
        return NetworkParameters.fromID(NetworkParameters.ID_MAINNET).equals(Network.NETWORK);
    }

    public HDKey(final byte[] seed) {
        mImpl = Wally.bip32_key_from_seed(seed, VER_PRIVATE);
    }

    // Temporary methods for use while converting from DeterministicKey
    static public DeterministicKey deriveChildKey(final DeterministicKey parent, final Integer childNum) {
        return HDKeyDerivation.deriveChildKey(parent, new ChildNumber(childNum));
    }

    static public DeterministicKey createMasterKeyFromSeed(final byte[] seed) {
        return HDKeyDerivation.createMasterPrivateKey(seed);
    }

    static public DeterministicKey createMasterKey(final byte[] chainCode, final byte[] publicKey) {
        final ECKey pub = ECKey.fromPublicOnly(publicKey);
        return new DeterministicKey(new ImmutableList.Builder<ChildNumber>().build(),
                                    chainCode, pub.getPubKeyPoint(), null, null);
    }

    static public DeterministicKey createMasterKey(final String chainCode, final String publicKey) {
        return createMasterKey(Wally.hex_to_bytes(chainCode), Wally.hex_to_bytes(publicKey));
    }
}
