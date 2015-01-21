package com.greenaddress.greenapi;

import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.engines.AESFastEngine;
import org.spongycastle.crypto.modes.CBCBlockCipher;
import org.spongycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.crypto.params.ParametersWithIV;

import java.security.SecureRandom;

public class AES256 {

    public static byte[] encrypt(final byte[] data, final byte[] key) throws InvalidCipherTextException {
        final PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESFastEngine()));
        // Random iv
        final SecureRandom rng = new SecureRandom();
        final byte[] ivBytes = new byte[16];
        rng.nextBytes(ivBytes);

        cipher.init(true, new ParametersWithIV(new KeyParameter(key), ivBytes));
        final byte[] outBuf = new byte[cipher.getOutputSize(data.length)];

        int processed = cipher.processBytes(data, 0, data.length, outBuf, 0);
        processed += cipher.doFinal(outBuf, processed);

        final byte[] outBuf2 = new byte[processed + 16];        // Make room for iv
        System.arraycopy(ivBytes, 0, outBuf2, 0, 16);    // Add iv
        System.arraycopy(outBuf, 0, outBuf2, 16, processed);    // Then the encrypted data

        return outBuf2;
    }

    public static byte[] decrypt(final byte[] data, final byte[] key) throws InvalidCipherTextException {
        final PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESFastEngine()));
        final byte[] ivBytes = new byte[16];
        System.arraycopy(data, 0, ivBytes, 0, ivBytes.length); // Get iv from data
        final byte[] dataonly = new byte[data.length - ivBytes.length];
        System.arraycopy(data, ivBytes.length, dataonly, 0, data.length - ivBytes.length);

        cipher.init(false, new ParametersWithIV(new KeyParameter(key), ivBytes));
        final byte[] decrypted = new byte[cipher.getOutputSize(dataonly.length)];
        final int len = cipher.processBytes(dataonly, 0, dataonly.length, decrypted, 0);
        cipher.doFinal(decrypted, len);
        return decrypted;
    }
}
