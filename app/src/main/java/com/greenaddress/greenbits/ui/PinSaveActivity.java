package com.greenaddress.greenbits.ui;

import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.security.keystore.UserNotAuthenticatedException;
import android.support.annotation.NonNull;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.dd.CircularProgressButton;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.greenaddress.greenapi.PinData;

import org.bitcoinj.crypto.MnemonicCode;

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
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.annotation.Nullable;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;


public class PinSaveActivity extends ActionBarActivity implements Observer {

    @NonNull private static final String KEYSTORE_KEY = "NativeAndroidAuth";

    private static final int SECONDS_AUTH_VALID = 10;
    private static final int ACTIVITY_REQUEST_CODE = 1;


    private void setPin(@NonNull final String pinText) {
        if (pinText.length() < 4) {
            Toast.makeText(PinSaveActivity.this, "PIN has to be between 4 and 15 digits", Toast.LENGTH_SHORT).show();
            return;
        }
        final String mnemonic_str = getIntent().getStringExtra("com.greenaddress.greenbits.NewPinMnemonic");
        final List<String> mnemonic = java.util.Arrays.asList(mnemonic_str.split(" "));
        final Button pinSkipButton = (Button) findViewById(R.id.pinSkipButton);
        final CircularProgressButton pinSaveButton = (CircularProgressButton) findViewById(R.id.pinSaveButton);

        pinSaveButton.setIndeterminateProgressMode(true);
        pinSaveButton.setProgress(50);
        pinSkipButton.setVisibility(View.GONE);

        Futures.addCallback(getGAService().setPin(MnemonicCode.toSeed(mnemonic, ""), mnemonic_str,
                        pinText, "default"),
                new FutureCallback<PinData>() {
                    @Override
                    public void onSuccess(@Nullable final PinData result) {
                        final SharedPreferences.Editor editor = getSharedPreferences("pin", MODE_PRIVATE).edit();
                        editor.putString("ident", result.ident);
                        editor.putInt("counter", 0);
                        editor.putString("encrypted", result.encrypted);
                        editor.apply();
                        final Intent tabbedMainActivity = new Intent(PinSaveActivity.this, TabbedMainActivity.class);
                        startActivity(tabbedMainActivity);
                        PinSaveActivity.this.finish();
                    }

                    @Override
                    public void onFailure(@NonNull final Throwable t) {
                        PinSaveActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pinSaveButton.setProgress(0);
                                pinSkipButton.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                }, getGAService().es);
    }

    @Override
    public void onResume() {
        super.onResume();
        getGAApp().getConnectionObservable().addObserver(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getGAApp().getConnectionObservable().deleteObserver(this);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void createKey(final boolean deleteImmediately) {
        KeyStore keyStore = null;
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            final KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");

            keyGenerator.init(new KeyGenParameterSpec.Builder(KEYSTORE_KEY,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
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

    @TargetApi(Build.VERSION_CODES.M)
    private void tryEncrypt() {

        createKey(false);
        final byte[] fakePin = new byte[32];
        new SecureRandom().nextBytes(fakePin);
        try {
            final KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            final SecretKey secretKey = (SecretKey) keyStore.getKey(KEYSTORE_KEY, null);
            final Cipher cipher = Cipher.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/"
                            + KeyProperties.ENCRYPTION_PADDING_PKCS7);

            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            final byte[] encryptedPIN = cipher.doFinal(fakePin);
            final SharedPreferences.Editor editor = getSharedPreferences("pin", MODE_PRIVATE).edit();
            final byte[] iv = cipher.getParameters().getParameterSpec(IvParameterSpec.class).getIV();
            editor.putString("native", Base64.encodeToString(encryptedPIN, Base64.NO_WRAP));
            editor.putString("nativeiv", Base64.encodeToString(iv, Base64.NO_WRAP));

            editor.apply();
            setPin(Base64.encodeToString(fakePin, Base64.NO_WRAP).substring(0, 15));
        } catch (@NonNull final UserNotAuthenticatedException e) {
            showAuthenticationScreen();
        } catch (@NonNull final KeyPermanentlyInvalidatedException e) {
            Toast.makeText(this, "Problem with key "
                            + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        } catch (@NonNull final InvalidParameterSpecException | BadPaddingException | IllegalBlockSizeException | KeyStoreException |
                CertificateException | UnrecoverableKeyException | IOException
                | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
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
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (requestCode == ACTIVITY_REQUEST_CODE) {
            // Challenge completed, proceed with using cipher
            if (resultCode == RESULT_OK) {
                tryEncrypt();
            }
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_save);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            try {
                createKey(true);

                final CheckBox pinSaveText = (CheckBox) findViewById(R.id.useNativeAuthentication);
                pinSaveText.setVisibility(View.VISIBLE);

                pinSaveText.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(final CompoundButton compoundButton, final boolean isChecked) {
                        if (isChecked) {
                            tryEncrypt();
                        }
                    }
                });

            } catch (@NonNull final RuntimeException e) {
                // lock not set, simply don't show native options
            }
        }
        final EditText pinSaveText = (EditText) findViewById(R.id.pinSaveText);

        final CircularProgressButton pinSaveButton = (CircularProgressButton) findViewById(R.id.pinSaveButton);
        final Button pinSkipButton = (Button) findViewById(R.id.pinSkipButton);

        pinSaveText.setOnEditorActionListener(
                new EditText.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(final TextView v, final int actionId, @NonNull final KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                                actionId == EditorInfo.IME_ACTION_DONE ||
                                event.getAction() == KeyEvent.ACTION_DOWN &&
                                        event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                            if (!event.isShiftPressed()) {
                                // the user is done typing.
                                setPin(pinSaveText.getText().toString());
                                return true; // consume.
                            }
                        }
                        return false; // pass on to other listeners.
                    }
                }
        );

        pinSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                setPin(pinSaveText.getText().toString());
            }
        });

        pinSkipButton.setOnClickListener(new View.OnClickListener() {
            // Skip
            @Override
            public void onClick(final View view) {
                final Intent tabbedActivity = new Intent(PinSaveActivity.this, TabbedMainActivity.class);
                startActivity(tabbedActivity);
                finish();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.common_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return item.getItemId() == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    @Override
    public void update(final Observable observable, final Object data) {

    }
}
