package com.greenaddress.greenbits.ui;

import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.security.keystore.KeyProperties;
import android.text.TextUtils;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.greenaddress.greenapi.GAException;
import com.greenaddress.greenapi.LoginData;
import com.greenaddress.greenapi.LoginFailed;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.ui.preferences.NetworkSettingsActivity;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Observable;
import java.util.Observer;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;


public class PinActivity extends LoginActivity implements Observer, View.OnClickListener {

    private Menu mMenu;
    private static final String KEYSTORE_KEY = "NativeAndroidAuth";
    private static final int ACTIVITY_REQUEST_CODE = 1;
    private CircularButton mPinLoginButton;
    private EditText mPinText;
    private TextView mPinError;

    private void login() {

        if (mPinLoginButton.isLoading())
            return;

        if (mPinText.length() < 4) {
            shortToast(R.string.pinErrorLength);
            return;
        }

        if (!mService.isConnected()) {
            toast(R.string.err_send_not_connected_will_resume);
            return;
        }

        mPinLoginButton.startLoading();
        mPinText.setEnabled(false);
        hideKeyboardFrom(mPinText);

        setUpLogin(UI.getText(mPinText), new Runnable() {
             public void run() {
                 UI.clear(mPinText);
                 mPinLoginButton.stopLoading();
                 UI.enable(mPinText);
                 UI.show(mPinError);
                 final int counter = mService.cfg("pin").getInt("counter", 1);
                 mPinError.setText(getString(R.string.attemptsLeft, 3 - counter));
              }
         });
     }

    private void setUpLogin(final String pin, final Runnable onFailureFn) {

        final AsyncFunction<Void, LoginData> connectToLogin = new AsyncFunction<Void, LoginData>() {
            @Override
            public ListenableFuture<LoginData> apply(final Void input) throws Exception {
                return mService.pinLogin(pin);
            }
        };

        final ListenableFuture<LoginData> loginFuture;
        loginFuture = Futures.transformAsync(mService.onConnected, connectToLogin, mService.getExecutor());

        Futures.addCallback(loginFuture, new FutureCallback<LoginData>() {
            @Override
            public void onSuccess(final LoginData result) {
                mService.cfgEdit("pin").putInt("counter", 0).apply();
                if (getCallingActivity() == null) {
                    onLoginSuccess();
                    return;
                }
                setResult(RESULT_OK);
                finishOnUiThread();
            }

            @Override
            public void onFailure(final Throwable t) {
                final String message;
                final SharedPreferences prefs = mService.cfg("pin");
                final int counter = prefs.getInt("counter", 0) + 1;

                final Throwable error;
                if (t instanceof GAException ||
                    Throwables.getRootCause(t) instanceof LoginFailed) {
                    final SharedPreferences.Editor editor = prefs.edit();
                    if (counter < 3) {
                        editor.putInt("counter", counter);
                        message = getString(R.string.attemptsLeftLong, 3 - counter);
                    } else {
                        message = getString(R.string.attemptsFinished);
                        editor.clear();
                    }
                    editor.apply();
                    error = null;
                } else {
                    error = t;
                    message = null;
                }

                PinActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        if (error != null)
                            PinActivity.this.toast(error);
                        else
                            PinActivity.this.toast(message);

                        if (counter >= 3) {
                            startActivity(new Intent(PinActivity.this, FirstScreenActivity.class));
                            finish();
                            return;
                        }
                        if (onFailureFn != null)
                            onFailureFn.run();
                    }
                });
            }
        }, mService.getExecutor());
    }

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {

        final SharedPreferences prefs = mService.cfg("pin");
        final String ident = prefs.getString("ident", null);

        if (ident == null) {
            startActivity(new Intent(this, FirstScreenActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_pin);
        mPinLoginButton = UI.find(this, R.id.pinLoginButton);
        mPinText = UI.find(this, R.id.pinText);
        mPinError = UI.find(this, R.id.pinErrorText);

        final String nativePIN = prefs.getString("native", null);

        if (TextUtils.isEmpty(nativePIN)) {

            mPinText.setOnEditorActionListener(
                    UI.getListenerRunOnEnter(new Runnable() {
                        public void run() {
                            login();
                        }
                    }));

            mPinLoginButton.setOnClickListener(this);
        } else  {
            mPinText.setEnabled(false);
            mPinLoginButton.startLoading();
            tryDecrypt();
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private Cipher getAESCipher() throws NoSuchAlgorithmException, NoSuchPaddingException {
        final String name = KeyProperties.KEY_ALGORITHM_AES + '/' +
                            KeyProperties.BLOCK_MODE_CBC + '/' +
                            KeyProperties.ENCRYPTION_PADDING_PKCS7;
        return Cipher.getInstance(name);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void tryDecrypt() {

        if (mService == null || mService.onConnected == null) {
            toast(R.string.unable_to_connect_to_service);
            finishOnUiThread();
            return;
        }

        final SharedPreferences prefs = mService.cfg("pin");
        final String nativePIN = prefs.getString("native", null);
        final String nativeIV = prefs.getString("nativeiv", null);

        try {
            final KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            final SecretKey secretKey = (SecretKey) keyStore.getKey(KEYSTORE_KEY, null);
            final Cipher cipher = getAESCipher();
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(Base64.decode(nativeIV, Base64.NO_WRAP)));
            final byte[] decrypted = cipher.doFinal(Base64.decode(nativePIN, Base64.NO_WRAP));
            final String pin = Base64.encodeToString(decrypted, Base64.NO_WRAP).substring(0, 15);

            Futures.addCallback(mService.onConnected, new FutureCallback<Void>() {
                @Override
                public void onSuccess(final Void result) {

                    if (mService.isConnected()) {
                        setUpLogin(pin, null);
                        return;
                    }

                    // try again
                    tryDecrypt();
                }

                @Override
                public void onFailure(final Throwable t) {
                    finishOnUiThread();
                }
            });
        } catch (final KeyStoreException | InvalidKeyException e) {
            showAuthenticationScreen();
        } catch (final InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException  |
                 CertificateException | UnrecoverableKeyException | IOException |
                 NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
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
                mService.setUserCancelledPINEntry(true);
                startActivity(new Intent(this, FirstScreenActivity.class));
                finish();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void showAuthenticationScreen() {
        final KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        final Intent intent = keyguardManager.createConfirmDeviceCredentialIntent(null, null);
        if (intent != null) {
            startActivityForResult(intent, ACTIVITY_REQUEST_CODE);
        }
    }

    @Override
    public void onResumeWithService() {
        mService.addConnectionObserver(this);
        super.onResumeWithService();
    }

    @Override
    public void onPauseWithService() {
        mService.deleteConnectionObserver(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        UI.unmapClick(mPinLoginButton);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.common_menu, menu);
        getMenuInflater().inflate(R.menu.preauth_menu, menu);
        mMenu = menu;
        final boolean connected = mService != null && mService.isConnected();
        setMenuItemVisible(mMenu, R.id.network_unavailable, !connected);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch(item.getItemId()) {
            case R.id.network_unavailable:
                return true;
            case R.id.network_preferences:
                startActivity(new Intent(this, NetworkSettingsActivity.class));
                return true;
            case R.id.watchonly_preference:
                startActivity(new Intent(PinActivity.this, WatchOnlyLoginActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void update(final Observable observable, final Object data) {
        final GaService.State state = (GaService.State) data;
        setMenuItemVisible(mMenu, R.id.network_unavailable,
                           !state.isConnected() && !state.isLoggedOrLoggingIn());
    }

    @Override
    public void onClick(final View v) {
        if (v == mPinLoginButton)
            login();
    }
 }
