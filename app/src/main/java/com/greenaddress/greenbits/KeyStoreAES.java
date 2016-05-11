package com.greenaddress.greenbits;
import com.greenaddress.greenapi.CryptoHelper;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.security.keystore.UserNotAuthenticatedException;
import android.support.annotation.NonNull;
import android.util.Base64;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidParameterSpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class KeyStoreAES {

    private static final String KEYSTORE_KEY = "NativeAndroidAuth";
    private static final int SECONDS_AUTH_VALID = 10;
    private static final int ACTIVITY_REQUEST_CODE = 1;

    @TargetApi(Build.VERSION_CODES.M)
    public static void createKey(final boolean deleteImmediately) {

        KeyStore keyStore = null;
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            final KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");

            keyGenerator.init(new KeyGenParameterSpec.Builder(KEYSTORE_KEY,
                    KeyProperties.PURPOSE_ENCRYPT
                            | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setUserAuthenticationValidityDurationSeconds(SECONDS_AUTH_VALID)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());
            keyGenerator.generateKey();


        } catch (@NonNull final NoSuchAlgorithmException | NoSuchProviderException
                | InvalidAlgorithmParameterException | KeyStoreException
                | CertificateException | IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (deleteImmediately && keyStore != null) {
                try {
                    keyStore.deleteEntry(KEYSTORE_KEY);
                } catch (@NonNull final KeyStoreException e) {
                }
            }
        }
    }


    public static class  RequiresAuthenticationScreen extends RuntimeException {}
    public static class  KeyInvalidated extends RuntimeException {}

    @TargetApi(Build.VERSION_CODES.M)
    @NonNull
    public static String tryEncrypt(final Context ctx) {
        createKey(false);
        final byte[] fakePin = CryptoHelper.randomBytes(32);
        try {
            final KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            final SecretKey secretKey = (SecretKey) keyStore.getKey(KEYSTORE_KEY, null);
            final Cipher cipher = Cipher.getInstance(
                    android.security.keystore.KeyProperties.KEY_ALGORITHM_AES + "/"
                            + android.security.keystore.KeyProperties.BLOCK_MODE_CBC + "/"
                            + android.security.keystore.KeyProperties.ENCRYPTION_PADDING_PKCS7);

            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            final byte[] encryptedPIN = cipher.doFinal(fakePin);
            final SharedPreferences.Editor editor = ctx.getSharedPreferences("pin", Context.MODE_PRIVATE).edit();
            final byte[] iv = cipher.getParameters().getParameterSpec(IvParameterSpec.class).getIV();
            editor.putString("native", Base64.encodeToString(encryptedPIN, Base64.NO_WRAP));
            editor.putString("nativeiv", Base64.encodeToString(iv, Base64.NO_WRAP));

            editor.apply();
            return Base64.encodeToString(fakePin, Base64.NO_WRAP).substring(0, 15);
        } catch (@NonNull final UserNotAuthenticatedException e) {
            throw new RequiresAuthenticationScreen();
        } catch (@NonNull final KeyPermanentlyInvalidatedException e) {
            throw new KeyInvalidated();
        } catch (@NonNull final InvalidParameterSpecException | BadPaddingException | IllegalBlockSizeException | KeyStoreException |
                CertificateException | UnrecoverableKeyException | IOException
                | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void showAuthenticationScreen(final Activity act) {
        final KeyguardManager keyguardManager = (KeyguardManager) act.getSystemService(Context.KEYGUARD_SERVICE);
        final Intent intent = keyguardManager.createConfirmDeviceCredentialIntent(null, null);
        if (intent != null) {
            act.startActivityForResult(intent, ACTIVITY_REQUEST_CODE);
        }
    }
}
