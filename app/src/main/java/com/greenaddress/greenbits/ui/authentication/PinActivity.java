package com.greenaddress.greenbits.ui.authentication;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.security.keystore.KeyProperties;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.util.Base64;
import android.view.MenuItem;
import android.view.WindowManager;

import com.blockstream.libgreenaddress.GDK;
import com.greenaddress.greenbits.KeyStoreAES;
import com.greenaddress.greenbits.ui.LoginActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;


public class PinActivity extends LoginActivity implements PinFragment.OnPinListener {

    private static final int ACTIVITY_REQUEST_CODE = 1;
    private PinFragment mPinFragment;

    private void login(final String pin) {

        if (isLoading())
            return;

        if (pin.length() < 4) {
            shortToast(R.string.id_pin_has_to_be_between_4_and_15);
            if (mPinFragment != null)
                mPinFragment.clear();
            return;
        }

        startLoading();
        if (mPinFragment != null)
            mPinFragment.setEnabled(false);

        loginWithPin(pin);

    }

    @Override
    protected void onLoginFailure() {
        super.onLoginFailure();
        final String message;
        final SharedPreferences prefs = mService.cfgPin();
        final int counter = prefs.getInt("counter", 0) + 1;
        final SharedPreferences.Editor editor = prefs.edit();
        final Exception lastLoginException = mService.getConnectionManager().getLastLoginException();
        final int code = getCode(lastLoginException);
        if (code == GDK.GA_NOT_AUTHORIZED) {
            if (counter < 3) {
                editor.putInt("counter", counter);
                message = getString(R.string.id_invalid_pin_you_have_1d, 3 - counter);
            } else {
                message = getString(R.string.id_invalid_pin_you_dont_have_any);
                editor.clear();
            }
            editor.apply();
        } else if (code == GDK.GA_RECONNECT || code == GDK.GA_ERROR) {
            message = getString(R.string.id_you_are_not_connected_to_the);
        } else if (lastLoginException != null) {
            message = UI.i18n(getResources(), lastLoginException.getMessage());
        } else {
            // Should not happen
            message = getString(R.string.id_error);
        }
        mService.getConnectionManager().clearPreviousLoginError();

        PinActivity.this.runOnUiThread(() -> {
            PinActivity.this.toast(message);

            if (counter >= 3) {
                startActivity(new Intent(PinActivity.this, FirstScreenActivity.class));
                finish();
                return;
            }
            stopLoading();
            if (mPinFragment != null) {
                mPinFragment.clear();
                mPinFragment.setEnabled(true);
            }
        });

    }

    @Override
    protected void onLoginSuccess() {
        super.onLoginSuccess();
        mService.cfgPin().edit().putInt("counter", 0).apply();
    }

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {

        final SharedPreferences prefs = mService.cfgPin();

        final String ident = prefs.getString("ident", null);

        if (ident == null) {
            startActivity(new Intent(this, FirstScreenActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_pin);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitleBackTransparent();

        if (isNativePin()) {
            tryDecrypt();
            return;
        }

        mPinFragment = new PinFragment();
        final Bundle bundle = new Bundle();
        bundle.putBoolean("is_six_digit", isSixDigit());
        mPinFragment.setArguments(bundle);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (requestCode == ACTIVITY_REQUEST_CODE) {
            // Challenge completed, proceed with using cipher
            if (resultCode == RESULT_OK) {
                tryDecrypt();
            } else {
                // The user canceled or didn’t complete the lock screen
                // operation. Go back to the initial login screen to allow
                // them to enter mnemonics.
                startActivity(new Intent(this, FirstScreenActivity.class));
                finish();
            }
        }
    }

    @Override
    public void onResumeWithService() {
        setAppNameTitle();

        if (mPinFragment != null)
            getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragment_container, mPinFragment).commit();
        super.onResumeWithService();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            onBackPressed();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPinInserted(final String pin) {
        if (mService != null) {
            mService.rescheduleDisconnect();
        }
        login(pin);
    }

    @Override
    public void onPinBackPressed() {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        final Intent intent = new Intent(this, FirstScreenActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public boolean isSixDigit() {
        return mService.cfgPin().getBoolean("is_six_digit", false);
    }

    private boolean isNativePin() {
        return !TextUtils.isEmpty(mService.cfgPin().getString("native", null));
    }


    @TargetApi(Build.VERSION_CODES.M)
    private void tryDecrypt() {

        final SharedPreferences prefs = mService.cfgPin();
        final String nativePIN = prefs.getString("native", null);
        final String nativeIV = prefs.getString("nativeiv", null);

        try {
            final KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            SecretKey secretKey = (SecretKey) keyStore.getKey(KeyStoreAES.getKeyName(mService), null);
            if (secretKey == null) {
                // support old native authentication
                secretKey = (SecretKey) keyStore.getKey(KeyStoreAES.KEYSTORE_KEY, null);
            }
            final Cipher cipher = getAESCipher();
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(Base64.decode(nativeIV, Base64.NO_WRAP)));
            final byte[] decrypted = cipher.doFinal(Base64.decode(nativePIN, Base64.NO_WRAP));
            final String pin = Base64.encodeToString(decrypted, Base64.NO_WRAP).substring(0, 15);
            login(pin);
        } catch (final KeyStoreException | InvalidKeyException e) {
            KeyStoreAES.showAuthenticationScreen(this);
        } catch (final InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException |
                 CertificateException | UnrecoverableKeyException | IOException |
                 NoSuchAlgorithmException | NoSuchPaddingException e) {
            // TODO: add exception message
            throw new RuntimeException(e);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private Cipher getAESCipher() throws NoSuchAlgorithmException, NoSuchPaddingException {
        final String name = KeyProperties.KEY_ALGORITHM_AES + '/' +
                            KeyProperties.BLOCK_MODE_CBC + '/' +
                            KeyProperties.ENCRYPTION_PADDING_PKCS7;
        return Cipher.getInstance(name);
    }
}
