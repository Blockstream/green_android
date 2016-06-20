package com.greenaddress.greenapi;

import android.util.Base64;
import com.blockstream.libwally.Wally;
import org.codehaus.jackson.map.MappingJsonFactory;

import java.util.Arrays;
import java.util.Map;

public class PinData {
    public final String mPinIdentifier;
    public byte[] mSalt;
    public byte[] mEncryptedData;
    public byte[] mSeed;
    public String mMnemonic;

    public PinData(final String pinIdentifier, final String encryptedData, final byte[] password) {
        mPinIdentifier = pinIdentifier;
        final String[] split = encryptedData.split(";");
        mSalt = split[0].getBytes(); // Note: Not decoded from base64!
        mEncryptedData = Base64.decode(split[1], Base64.NO_WRAP);

        final byte[] hash = Wally.pbkdf2_hmac_sha512(password, mSalt, 0, 2048);
        final byte[] key = Arrays.copyOf(hash, 32);

        final byte[] decrypted = CryptoHelper.decrypt_aes_cbc(mEncryptedData, key);
        final Map<String, String> json;
        try {
            json = new MappingJsonFactory().getCodec().readValue(new String(decrypted), Map.class);
        } catch (final java.io.IOException e) {
            // Can only happen if the app is coded incorrectly, will be
            // caught when attempting to use the seed/mnemonic.
            return;
        }
        mSeed = Wally.hex_to_bytes(json.get("seed"));
        mMnemonic = json.get("mnemonic");
    }

    public PinData(final String pinIdentifier) {
        mPinIdentifier = pinIdentifier;
        mSalt = null;
        mEncryptedData = null;
        mSeed = null;
        mMnemonic = null;
    }

    public void encrypt(final byte[] json, final byte[] password) {

        mSalt = Base64.encode(CryptoHelper.randomBytes(16), Base64.NO_WRAP);

        final byte[] hash = Wally.pbkdf2_hmac_sha512(password, mSalt, 0, 2048);
        final byte[] key = Arrays.copyOf(hash, 32);

        mEncryptedData = CryptoHelper.encrypt_aes_cbc(json, key);
    }
}
