package com.greenaddress.greenapi;

import com.blockstream.libwally.Wally;

import java.security.SecureRandom;

public class CryptoHelper {

    private final static Object WL = Wally.bip39_get_wordlist("en");

    public static byte[] randomBytes(final int len) {
        final byte[] b = new byte[len];
        new SecureRandom().nextBytes(b);
        return b;
    }

    public static boolean initialize() {
        try {
            Wally.init(0);
            Wally.secp_randomize(randomBytes(Wally.WALLY_SECP_RANDOMIZE_LEN));
            return true;
        } catch (final Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String decrypt_mnemonic(final byte[] data, final String pass) {
        return CryptoHelper.mnemonic_from_bytes(MnemonicHelper.decryptMnemonic(data, pass));
    }

    public static String mnemonic_from_bytes(final byte[] data) {
        return Wally.bip39_mnemonic_from_bytes(WL, data);
    }
}
