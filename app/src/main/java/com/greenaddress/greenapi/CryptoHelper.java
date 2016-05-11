package com.greenaddress.greenapi;

import com.blockstream.libwally.Wally;
import com.greenaddress.greenbits.ui.MnemonicHelper;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.Arrays;

public class CryptoHelper {

    private final static int BL = Wally.AES_BLOCK_LEN;
    private final static Object WL = Wally.bip39_get_wordlist("en");

    public static byte[] randomBytes(int len) {
        final byte[] b = new byte[len];
        new SecureRandom().nextBytes(b);
        return b;
    }

    public static byte[] mnemonic_to_seed(final String mnemonic) {
        final byte[] seed = new byte[Wally.BIP39_SEED_LEN_512];
        final int written = Wally.bip39_mnemonic_to_seed(mnemonic, /*password*/null, seed);
        if (written != Wally.BIP39_SEED_LEN_512) throw new IllegalArgumentException();
        return seed;
    }

    public static byte[] mnemonic_to_bytes(final String mnemonic) {
        return mnemonic_to_bytes(mnemonic, Wally.BIP39_ENTROPY_LEN_256);
    }

    private static byte[] mnemonic_to_bytes(final String mnemonic, final int size) {
        final byte[] buf = new byte[size];
        final int len = Wally.bip39_mnemonic_to_bytes(WL, mnemonic, buf);
        if (len > buf.length) throw new IllegalArgumentException();
        return len == size ? buf: Arrays.copyOf(buf, len);
    }

    public static String encrypted_mnemonic_to_mnemonic(final String mnemonics, final String pass) {
        return mnemonic_from_bytes(MnemonicHelper.decryptMnemonic(
                mnemonic_to_bytes(mnemonics, Wally.BIP39_ENTROPY_LEN_288),
                Normalizer.normalize(pass, Normalizer.Form.NFC)));
    }

    public static String encrypted_mnemonic_to_mnemonic(final byte[] data, final String pass) {
        return CryptoHelper.mnemonic_from_bytes(MnemonicHelper.decryptMnemonic(data, pass));
    }

    public static String mnemonic_from_bytes(final byte[] data) {
        return Wally.bip39_mnemonic_from_bytes(WL, data);
    }

    public static byte[] encrypt_aes_cbc(final byte[] data, final byte[] key) {
        final byte[] iv = randomBytes(BL);
        final byte[] encrypted = new byte[((data.length / BL) + 1) * BL];
        final int written = Wally.aes_cbc(key, iv, data,
                Wally.AES_FLAG_ENCRYPT, encrypted);
        if (written != encrypted.length)
            throw new IllegalArgumentException("Encrypt failed");
        final byte[] ivAndEncrypted = new byte[BL + written];
        System.arraycopy(iv, 0, ivAndEncrypted, 0, BL);
        System.arraycopy(encrypted, 0, ivAndEncrypted, BL, written);
        return ivAndEncrypted;
    }

    public static byte[] decrypt_aes_cbc(final byte[] ivAndData, final byte[] key) {
        final byte[] iv = new byte[BL];
        System.arraycopy(ivAndData, 0, iv, 0, BL);
        final byte[] dataNoIv = new byte[ivAndData.length - BL];
        System.arraycopy(ivAndData, BL, dataNoIv, 0, ivAndData.length - BL);
        final byte[] tmpDecrypted = new byte[dataNoIv.length];

        final int written = Wally.aes_cbc(key, iv, dataNoIv,
                Wally.AES_FLAG_DECRYPT, tmpDecrypted);
        if (written > tmpDecrypted.length || tmpDecrypted.length - written > BL)
            throw new IllegalArgumentException("Decrypt failed");

        final byte[] plaintext = new byte[written];
        System.arraycopy(tmpDecrypted, 0, plaintext, 0, written);

        return plaintext;
    }
}
