package com.greenaddress.greenapi;

import android.util.Base64;

import com.greenaddress.greenapi.JSONMap;
import com.blockstream.libwally.Wally;

import java.util.HashMap;
import java.util.Map;

public class PinData {
    public final String mPinIdentifier;
    public final byte[] mSalt;
    public final byte[] mEncryptedData;
    public final byte[] mSeed;
    public final String mMnemonic;

    private PinData(final String pinIdentifier, final byte[] salt, final byte[] encryptedJSON,
                    final byte[] seed, final String mnemonic) {
        mPinIdentifier = pinIdentifier;
        mSalt = salt;
        mEncryptedData = encryptedJSON;
        mSeed = seed;
        mMnemonic = mnemonic;
    }

    public static PinData fromEncrypted(final String pinIdentifier, final byte[] salt, final byte[] encryptedJSON, final byte[] password) {

        final JSONMap json = CryptoHelper.decryptJSON(encryptedJSON, password, salt);
        return new PinData(pinIdentifier, salt, encryptedJSON,
                           json.getBytes("seed"), json.getString("mnemonic"));
    }

    public static PinData fromMnemonic(final String pinIdentifier, final String mnemonic, final byte[] password) {

        final byte[] salt = Base64.encode(CryptoHelper.randomBytes(16), Base64.NO_WRAP);
        final byte[] seed = CryptoHelper.mnemonic_to_seed(mnemonic);

        final Map<String, Object> json = new HashMap<>();
        json.put("mnemonic", mnemonic);
        json.put("seed", Wally.hex_from_bytes(seed));

        final byte[] encryptedJSON = CryptoHelper.encryptJSON(new JSONMap(json), password, salt);

        return new PinData(pinIdentifier, salt, encryptedJSON, seed, mnemonic);
    }
}
