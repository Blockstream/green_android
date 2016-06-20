package com.greenaddress.greenbits.ui;

import com.blockstream.libwally.Wally;
import org.bitcoinj.crypto.DeterministicKey;

import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.dd.CircularProgressButton;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.greenaddress.greenapi.GAException;
import com.greenaddress.greenapi.HDKey;
import com.greenaddress.greenapi.LoginData;
import com.greenaddress.greenapi.SWWallet;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.ui.preferences.ProxySettingsActivity;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;


public class PinActivity extends GaActivity implements Observer {

    private Menu mMenu;
    private static final String KEYSTORE_KEY = "NativeAndroidAuth";
    private static final int ACTIVITY_REQUEST_CODE = 1;


    private void login(final CircularProgressButton pinLoginButton, final EditText pinText, final TextView pinError) {
        final GaService service = mService;

        if (pinText.length() < 4) {
            shortToast("PIN has to be between 4 and 15 long");
            return;
        }

        if (!service.isConnected()) {
            toast(R.string.err_send_not_connected_will_resume);
            return;
        }

        pinLoginButton.setIndeterminateProgressMode(true);
        pinLoginButton.setProgress(50);
        pinText.setEnabled(false);

        final InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(pinText.getWindowToken(), 0);

        final AsyncFunction<Void, LoginData> connectToLogin = new AsyncFunction<Void, LoginData>() {
            @Override
            public ListenableFuture<LoginData> apply(final Void input) {
                return service.pinLogin(pinText.getText().toString());
            }
        };

        final ListenableFuture<LoginData> loginFuture = Futures.transform(service.onConnected, connectToLogin, service.es);

        Futures.addCallback(loginFuture, new FutureCallback<LoginData>() {
            @Override
            public void onSuccess(final LoginData result) {
                service.cfgEdit("pin").putInt("counter", 0).apply();
                if (getCallingActivity() == null) {
                    startActivity(new Intent(PinActivity.this, TabbedMainActivity.class));
                } else {
                    setResult(RESULT_OK);
                }
                finish();
            }

            @Override
            public void onFailure(final Throwable t) {
                String message = t.getMessage();
                final SharedPreferences prefs = service.cfg("pin");
                final int counter = prefs.getInt("counter", 0) + 1;
                if (t instanceof GAException) {
                    final SharedPreferences.Editor editor = prefs.edit();
                    if (counter < 3) {
                        editor.putInt("counter", counter);
                        message = getString(R.string.attemptsLeftLong, 3 - counter);
                    } else {
                        message = getString(R.string.attemptsFinished);
                        editor.clear();
                    }

                    editor.apply();
                }
                final String tstMsg = message;

                PinActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        PinActivity.this.toast(tstMsg);

                        if (counter >= 3) {
                            startActivity(new Intent(PinActivity.this, FirstScreenActivity.class));
                            finish();
                            return;
                        }
                        pinText.setText("");
                        pinLoginButton.setProgress(0);
                        pinText.setEnabled(true);
                        pinError.setVisibility(View.VISIBLE);
                        pinError.setText(getString(R.string.attemptsLeft, 3 - counter));
                    }
                });
            }
        }, service.es);
    }

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {
        final GaService service = mService;

        final SharedPreferences prefs = service.cfg("pin");
        final String ident = prefs.getString("ident", null);
        final String androidLogin = prefs.getString("native", null);

        if (androidLogin == null && ident != null) {
            setContentView(R.layout.activity_pin);

            final EditText pinText = (EditText) findViewById(R.id.pinText);
            final CircularProgressButton pinLoginButton = (CircularProgressButton) findViewById(R.id.pinLoginButton);
            final TextView pinError = (TextView) findViewById(R.id.pinErrorText);

            pinText.setOnEditorActionListener(
                    new EditText.OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(final TextView v, final int actionId, final KeyEvent event) {
                            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                                    actionId == EditorInfo.IME_ACTION_DONE ||
                                    (event != null && event.getAction() == KeyEvent.ACTION_DOWN) &&
                                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                                if (event == null || !event.isShiftPressed()) {
                                    // the user is done typing.
                                    if (!pinText.getText().toString().isEmpty()) {
                                        login(pinLoginButton, pinText, pinError);
                                        return true; // consume.
                                    }
                                }
                            }
                            return false; // pass on to other listeners.
                        }
                    }
            );

            pinLoginButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    login(pinLoginButton, pinText, pinError);
                }
            });

        } else if (androidLogin != null && ident != null) {
            tryDecrypt();
        } else {
            startActivity(new Intent(this, FirstScreenActivity.class));
            finish();
        }
    }

    private Cipher getAESCipher() throws NoSuchAlgorithmException, NoSuchPaddingException {
        final String name = KeyProperties.KEY_ALGORITHM_AES + "/" +
                            KeyProperties.BLOCK_MODE_CBC + "/" +
                            KeyProperties.ENCRYPTION_PADDING_PKCS7;
        return Cipher.getInstance(name);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void tryDecrypt() {

        final GaService service = mService;
        if (service.onConnected == null) {
            finish();
            return;
        }

        final SharedPreferences prefs = service.cfg("pin");
        final String androidLogin = prefs.getString("native", null);
        final String aesiv = prefs.getString("nativeiv", null);

        try {
            final KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            final SecretKey secretKey = (SecretKey) keyStore.getKey(KEYSTORE_KEY, null);
            final Cipher cipher = getAESCipher();
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(Base64.decode(aesiv, Base64.NO_WRAP)));
            final byte[] decrypted = cipher.doFinal(Base64.decode(androidLogin, Base64.NO_WRAP));
            final String pin = Base64.encodeToString(decrypted, Base64.NO_WRAP).substring(0, 15);

            Futures.addCallback(service.onConnected, new FutureCallback<Void>() {
                @Override
                public void onSuccess(final Void result) {

                    if (!service.isConnected()) {
                        PinActivity.this.toast("Failed to connect, please reopen the app to authenticate");
                        finish();
                        return;
                    }

                    final AsyncFunction<Void, LoginData> connectToLogin = new AsyncFunction<Void, LoginData>() {
                        @Override
                        public ListenableFuture<LoginData> apply(final Void input) {
                            return service.pinLogin(pin);
                        }
                    };

                    final ListenableFuture<LoginData> loginFuture = Futures.transform(service.onConnected, connectToLogin, service.es);

                    Futures.addCallback(loginFuture, new FutureCallback<LoginData>() {
                        @Override
                        public void onSuccess(final LoginData result) {
                            final SharedPreferences.Editor editor = prefs.edit();
                            editor.putInt("counter", 0);
                            editor.apply();
                            if (getCallingActivity() == null) {
                                startActivity( new Intent(PinActivity.this, TabbedMainActivity.class));
                                finish();
                                return;
                            }
                            setResult(RESULT_OK);
                            finish();
                        }

                        @Override
                        public void onFailure(final Throwable t) {
                            String message = t.getMessage();
                            final int counter = prefs.getInt("counter", 0) + 1;
                            if (t instanceof GAException) {
                                final SharedPreferences.Editor editor = prefs.edit();
                                if (counter < 3) {
                                    editor.putInt("counter", counter);
                                    message = getString(R.string.attemptsLeftLong, 3 - counter);
                                } else {
                                    message = getString(R.string.attemptsFinished);
                                    editor.clear();
                                }

                                editor.apply();
                            }
                            final String toastMsg = message;

                            PinActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    PinActivity.this.toast(toastMsg);
                                    if (counter >= 3) {
                                        startActivity( new Intent(PinActivity.this, FirstScreenActivity.class));
                                        finish();
                                    }
                                }
                            });
                        }
                    }, service.es);
                }

                @Override
                public void onFailure(final Throwable t) {
                    finish();
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
                // operation. Go to error/cancellation flow.
                toast("Authentication not provided, closing.");
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
        final GaService service = mService;
        service.addConnectionObserver(this);

        if (service.isLoggedOrLoggingIn()) {
            // already logged in, could be from different app via intent
            startActivity(new Intent(this, TabbedMainActivity.class));
            finish();
        }
    }

    @Override
    public void onPauseWithService() {
        final GaService service = mService;
        service.deleteConnectionObserver(this);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.common_menu, menu);

        // disable proxy until fully working
        // getMenuInflater().inflate(R.menu.proxy_menu, menu);

        mMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch(item.getItemId()) {
            case R.id.network_unavailable:
                return true;
            case R.id.proxy_preferences:
                startActivity(new Intent(this, ProxySettingsActivity.class));
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
}
