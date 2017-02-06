package com.greenaddress.greenapi;

import android.util.Base64;

import com.blockstream.libwally.Wally;

import org.codehaus.jackson.map.MappingJsonFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PinData {
    public final String mPinIdentifier;
    public final byte[] mSalt;
    public final byte[] mEncryptedData;
    public final byte[] mSeed;
    public final String mMnemonic;

    private PinData(final String pinIdentifier, final byte[] salt, final byte[] encryptedData,
                    final byte[] seed, final String mnemonic) {
        mPinIdentifier = pinIdentifier;
        mSalt = salt;
        mEncryptedData = encryptedData;
        mSeed = seed;
        mMnemonic = mnemonic;
    }

    public static PinData fromEncrypted(final String pinIdentifier, final byte[] salt, final byte[] encryptedData, final byte[] password) {

        final byte[] decrypted = CryptoHelper.decrypt_aes_cbc(encryptedData, password, salt);
        final Map<String, String> json;
        try {
            json = new MappingJsonFactory().getCodec().readValue(new String(decrypted), Map.class);
        } catch (final java.io.IOException e) {
            // Can only happen if the app is coded incorrectly
            throw new RuntimeException(e);
        }
        final byte[] seed = Wally.hex_to_bytes(json.get("seed"));
        final String mnemonic = json.get("mnemonic");

        return new PinData(pinIdentifier, salt, encryptedData, seed, mnemonic);
    }

    public static PinData fromMnemonic(final String pinIdentifier, final String mnemonic, final byte[] password) {

        final byte[] salt = CryptoHelper.randomBytes(16);
        final byte[] seed = CryptoHelper.mnemonic_to_seed(mnemonic);

        final Map<String, String> out = new HashMap<>();
        out.put("mnemonic", mnemonic);
        out.put("seed", Wally.hex_from_bytes(seed));

        final ByteArrayOutputStream b = new ByteArrayOutputStream();
        try {
            new MappingJsonFactory().getCodec().writeValue(b, out);
        } catch (final IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        final byte[] json = b.toByteArray();

        final byte[] encryptedData = CryptoHelper.encrypt_aes_cbc(json, password,
                                                                  Base64.encode(salt, Base64.NO_WRAP));
        return new PinData(pinIdentifier, salt, encryptedData, seed, mnemonic);
    }
}
