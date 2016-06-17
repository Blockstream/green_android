package com.greenaddress.greenapi;

import android.util.Base64;
import com.blockstream.libwally.Wally;
import org.codehaus.jackson.map.MappingJsonFactory;

import java.util.Arrays;
import java.util.Map;

public class PinData {
    public final String mPinIdentifier;
    public String mEncryptedData;
    public byte[] mSeed;
    public String mMnemonic;

    public PinData(final String pinIdentifier, final String encryptedData) {
        mPinIdentifier = pinIdentifier;
        mEncryptedData = encryptedData;
        mSeed = null;
        mMnemonic = null;
    }

    public void decrypt(final byte[] password) {

        final String[] split = mEncryptedData.split(";");
        final byte[] encrypted = Base64.decode(split[1], Base64.NO_WRAP);

        final byte[] hash = Wally.pbkdf2_hmac_sha512(password, split[0].getBytes(), 0, 2048);
        final byte[] key = Arrays.copyOf(hash, 32);

        final byte[] decrypted = CryptoHelper.decrypt_aes_cbc(encrypted, key);
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
}
