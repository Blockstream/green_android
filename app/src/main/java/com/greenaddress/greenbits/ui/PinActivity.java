package com.greenaddress.greenbits.ui;

import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.security.keystore.KeyProperties;
import android.support.annotation.NonNull;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.dd.CircularProgressButton;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.greenaddress.greenapi.GAException;
import com.greenaddress.greenapi.LoginData;
import com.greenaddress.greenapi.PinData;
import com.greenaddress.greenbits.ConnectivityObservable;
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
import java.util.Observable;
import java.util.Observer;

import javax.annotation.Nullable;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;


public class PinActivity extends ActionBarActivity implements Observer {

    private Menu menu;
    @NonNull private static final String KEYSTORE_KEY = "NativeAndroidAuth";
    private static final int ACTIVITY_REQUEST_CODE = 1;


    private void login(@NonNull final CircularProgressButton pinLoginButton, final String ident, final EditText pinText, @NonNull final TextView pinError) {
        if (pinText.length() < 4) {
            Toast.makeText(PinActivity.this, "PIN has to be between 4 and 15 long", Toast.LENGTH_SHORT).show();
            return;
        }
        Futures.addCallback(getGAApp().onServiceAttached, new FutureCallback<Void>() {
            @Override
            public void onSuccess(final @Nullable Void result) {
                loginAfterServiceConnected(pinLoginButton, ident, pinText, pinError);
            }

            @Override
            public void onFailure(@NonNull final Throwable t) {
                t.printStackTrace();
                Toast.makeText(PinActivity.this, getString(R.string.err_send_not_connected_will_resume), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loginAfterServiceConnected(@NonNull final CircularProgressButton pinLoginButton, final String ident, final EditText pinText, @NonNull final TextView pinError) {
        final ConnectivityObservable.State state = getGAApp().getConnectionObservable().getState();
        if (!state.equals(ConnectivityObservable.State.CONNECTED)) {
            Toast.makeText(PinActivity.this, getString(R.string.err_send_not_connected_will_resume), Toast.LENGTH_LONG).show();
            return;
        }

        final GaService gaService = getGAService();

        final PinData pinData = new PinData(ident,
                getSharedPreferences("pin", MODE_PRIVATE).getString("encrypted", null));

        pinLoginButton.setIndeterminateProgressMode(true);
        pinLoginButton.setProgress(50);
        pinText.setEnabled(false);

        final InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(pinText.getWindowToken(), 0);

        final AsyncFunction<Void, LoginData> connectToLogin = new AsyncFunction<Void, LoginData>() {
            @NonNull
            @Override
            public ListenableFuture<LoginData> apply(@Nullable final Void input) {
                return gaService.pinLogin(pinData, pinText.getText().toString());
            }
        };

        final ListenableFuture<LoginData> loginFuture = Futures.transform(gaService.onConnected, connectToLogin, gaService.es);

        Futures.addCallback(loginFuture, new FutureCallback<LoginData>() {
            @Override
            public void onSuccess(@Nullable final LoginData result) {
                final SharedPreferences.Editor editor = getSharedPreferences("pin", MODE_PRIVATE).edit();
                editor.putInt("counter", 0);
                editor.apply();
                if (getCallingActivity() == null) {
                    final Intent mainActivity = new Intent(PinActivity.this, TabbedMainActivity.class);
                    startActivity(mainActivity);
                    finish();
                } else {
                    setResult(RESULT_OK);
                    finish();
                }
            }

            @Override
            public void onFailure(@NonNull final Throwable t) {
                String message = t.getMessage();
                final SharedPreferences pref = getSharedPreferences("pin", MODE_PRIVATE);
                final int counter = pref.getInt("counter", 0) + 1;
                if (t instanceof GAException) {
                    final SharedPreferences.Editor editor = pref.edit();
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

                        Toast.makeText(PinActivity.this, tstMsg, Toast.LENGTH_LONG).show();

                        if (counter >= 3) {
                            final Intent firstScreenActivity = new Intent(PinActivity.this, FirstScreenActivity.class);
                            startActivity(firstScreenActivity);
                            finish();
                        } else {
                            pinText.setText("");
                            pinLoginButton.setProgress(0);
                            pinText.setEnabled(true);
                            pinError.setVisibility(View.VISIBLE);
                            pinError.setText(getString(R.string.attemptsLeft, 3 - counter));
                        }
                    }
                });
            }
        }, gaService.es);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String ident = getSharedPreferences("pin", MODE_PRIVATE).getString("ident", null);
        final String androidLogin = getSharedPreferences("pin", MODE_PRIVATE).getString("native", null);

        if (androidLogin == null && ident != null) {
            setContentView(R.layout.activity_pin);

            final EditText pinText = (EditText) findViewById(R.id.pinText);
            final CircularProgressButton pinLoginButton = (CircularProgressButton) findViewById(R.id.pinLoginButton);
            final TextView pinError = (TextView) findViewById(R.id.pinErrorText);

            pinText.setOnEditorActionListener(
                    new EditText.OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(final TextView v, final int actionId, @Nullable final KeyEvent event) {
                            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                                    actionId == EditorInfo.IME_ACTION_DONE ||
                                    (event != null && event.getAction() == KeyEvent.ACTION_DOWN) &&
                                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                                if (event == null || !event.isShiftPressed()) {
                                    // the user is done typing.
                                    if (!pinText.getText().toString().isEmpty()) {
                                        login(pinLoginButton, ident, pinText, pinError);
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
                    login(pinLoginButton, ident, pinText, pinError);
                }
            });

        } else if (androidLogin != null && ident != null) {
            tryDecrypt();
        } else {
            final Intent firstScreenActivity = new Intent(this, FirstScreenActivity.class);
            startActivity(firstScreenActivity);
            finish();
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void tryDecrypt() {

        final SharedPreferences prefs = getSharedPreferences("pin", MODE_PRIVATE);

        final String androidLogin = prefs.getString("native", null);
        final String aesiv = prefs.getString("nativeiv", null);

        final String ident = prefs.getString("ident", null);

        try {
            final KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            final SecretKey secretKey = (SecretKey) keyStore.getKey(KEYSTORE_KEY, null);
            final Cipher cipher = Cipher.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/"
                            + KeyProperties.ENCRYPTION_PADDING_PKCS7);

            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(Base64.decode(aesiv, Base64.NO_WRAP)));
            final byte[] decrypted = cipher.doFinal(Base64.decode(androidLogin, Base64.NO_WRAP));

            final GaService gaService = getGAApp().gaService;
            if (gaService != null && gaService.onConnected != null) {
                //Auxillary Future to make sure we are connected.
                Futures.addCallback(gaService.triggerOnFullyConnected, new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(@Nullable final Void result) {

                        Futures.addCallback(getGAApp().onServiceAttached, new FutureCallback<Void>() {
                            @Override
                            public void onSuccess(final @Nullable Void result) {
                                final ConnectivityObservable.State state = getGAApp().getConnectionObservable().getState();
                                if (!state.equals(ConnectivityObservable.State.CONNECTED)) {
                                    Toast.makeText(PinActivity.this, "Failed to connect, please reopen the app to authenticate", Toast.LENGTH_LONG).show();
                                    finish();
                                }

                                final PinData pinData = new PinData(ident,
                                        prefs.getString("encrypted", null));

                                final AsyncFunction<Void, LoginData> connectToLogin = new AsyncFunction<Void, LoginData>() {
                                    @NonNull
                                    @Override
                                    public ListenableFuture<LoginData> apply(@Nullable final Void input) {
                                        return gaService.pinLogin(pinData, Base64.encodeToString(decrypted, Base64.NO_WRAP).substring(0, 15));
                                    }
                                };

                                final ListenableFuture<LoginData> loginFuture = Futures.transform(gaService.onConnected, connectToLogin, gaService.es);

                                Futures.addCallback(loginFuture, new FutureCallback<LoginData>() {
                                    @Override
                                    public void onSuccess(@Nullable final LoginData result) {
                                        final SharedPreferences.Editor editor = prefs.edit();
                                        editor.putInt("counter", 0);
                                        editor.apply();
                                        if (getCallingActivity() == null) {
                                            final Intent mainActivity = new Intent(PinActivity.this, TabbedMainActivity.class);
                                            startActivity(mainActivity);
                                            finish();
                                        } else {
                                            setResult(RESULT_OK);
                                            finish();
                                        }
                                    }

                                    @Override
                                    public void onFailure(@NonNull final Throwable t) {
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
                                        final String tstMsg = message;

                                        PinActivity.this.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Toast.makeText(PinActivity.this, tstMsg, Toast.LENGTH_LONG).show();
                                                if (counter >= 3) {
                                                    final Intent firstScreenActivity = new Intent(PinActivity.this, FirstScreenActivity.class);
                                                    startActivity(firstScreenActivity);
                                                    finish();
                                                }
                                            }
                                        });
                                    }
                                }, gaService.es);                            }

                            @Override
                            public void onFailure(@NonNull final Throwable t) {
                                t.printStackTrace();
                                Toast.makeText(PinActivity.this, "Failed to connect, please reopen the app to authenticate", Toast.LENGTH_LONG).show();
                                finish();
                            }
                        });                    }

                    @Override
                    public void onFailure(@NonNull final Throwable t) {
                        finish();
                    }
                });
            } else {
                finish();
            }
        } catch (@NonNull final KeyStoreException | InvalidKeyException e) {
            showAuthenticationScreen();
        } catch (@NonNull final InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException  |
                CertificateException | UnrecoverableKeyException | IOException
                | NoSuchPaddingException | NoSuchAlgorithmException e) {
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
                Toast.makeText(PinActivity.this, "Authentication not provided, closing.", Toast.LENGTH_LONG).show();
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
    public void onResume() {
        super.onResume();
        getGAApp().getConnectionObservable().addObserver(this);

        if (getGAApp().getConnectionObservable().getState().equals(ConnectivityObservable.State.LOGGEDIN) || getGAApp().getConnectionObservable().getState().equals(ConnectivityObservable.State.LOGGINGIN)) {
            // already logged in, could be from different app via intent
            final Intent mainActivity = new Intent(PinActivity.this, TabbedMainActivity.class);
            startActivity(mainActivity);
            finish();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        getGAApp().getConnectionObservable().deleteObserver(this);
    }


    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.common_menu, menu);

        // disable proxy until fully working
        // getMenuInflater().inflate(R.menu.proxy_menu, menu);

        this.menu = menu;
        return true;
    }

    private void setPlugVisible(final boolean visible) {
        if (menu != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final MenuItem item = menu.findItem(R.id.network_unavailable);
                    item.setVisible(visible);
                    AnimationUtils.loadAnimation(PinActivity.this, R.anim.rotation);
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.network_unavailable) {
            Toast.makeText(PinActivity.this, getGAApp().getConnectionObservable().getState().toString(), Toast.LENGTH_LONG).show();
            return true;
        }

        if (id == R.id.proxy_preferences) {
            final Intent settingsActivity = new Intent(PinActivity.this, ProxySettingsActivity.class);
            startActivity(settingsActivity);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void update(final Observable observable, final Object data) {
        // connectivity changed
        final ConnectivityObservable.State currentState = getGAApp().getConnectionObservable().getState();
        if (menu != null) {
            setPlugVisible(currentState != ConnectivityObservable.State.CONNECTED && currentState != ConnectivityObservable.State.LOGGEDIN && currentState != ConnectivityObservable.State.LOGGINGIN);
        }
    }
}
