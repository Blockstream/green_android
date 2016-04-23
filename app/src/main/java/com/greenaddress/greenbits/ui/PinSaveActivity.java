package com.greenaddress.greenbits.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
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
import com.greenaddress.greenbits.KeyStoreAES;

import org.bitcoinj.crypto.MnemonicCode;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class PinSaveActivity extends ActionBarActivity implements Observer {

    private static final int ACTIVITY_REQUEST_CODE = 1;

    private void setPin(@NonNull final String pinText) {
        if (pinText.length() < 4) {
            Toast.makeText(PinSaveActivity.this, "PIN has to be between 4 and 15 digits", Toast.LENGTH_SHORT).show();
            return;
        }
        final InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        final EditText pinSaveText = (EditText) findViewById(R.id.pinSaveText);
        imm.hideSoftInputFromWindow(pinSaveText.getWindowToken(), 0);
        final String mnemonic_str = getIntent().getStringExtra("com.greenaddress.greenbits.NewPinMnemonic");
        final List<String> mnemonic = java.util.Arrays.asList(mnemonic_str.split(" "));
        final Button pinSkipButton = (Button) findViewById(R.id.pinSkipButton);
        final CircularProgressButton pinSaveButton = (CircularProgressButton) findViewById(R.id.pinSaveButton);

        pinSaveButton.setIndeterminateProgressMode(true);
        pinSaveButton.setProgress(50);
        pinSaveText.setEnabled(false);
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
                        setResult(RESULT_OK);
                        finish();
                    }

                    @Override
                    public void onFailure(@NonNull final Throwable t) {
                        PinSaveActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pinSaveButton.setProgress(0);
                                pinSaveText.setEnabled(true);
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

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        // Challenge completed, proceed with using cipher
        if (requestCode == ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                setPin(KeyStoreAES.tryEncrypt(this));
            } catch (final KeyStoreAES.RequiresAuthenticationScreen e) {
                KeyStoreAES.showAuthenticationScreen(this);
            } catch (final KeyStoreAES.KeyInvalidated e) {
                Toast.makeText(this, "Problem with key "
                        + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_save);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            try {
                KeyStoreAES.createKey(true);

                final CheckBox nativeAuth = (CheckBox) findViewById(R.id.useNativeAuthentication);
                nativeAuth.setVisibility(View.VISIBLE);

                nativeAuth.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(final CompoundButton compoundButton, final boolean isChecked) {
                        if (isChecked) {
                            try {
                                setPin(KeyStoreAES.tryEncrypt(PinSaveActivity.this));
                            } catch (final KeyStoreAES.RequiresAuthenticationScreen e) {
                                KeyStoreAES.showAuthenticationScreen(PinSaveActivity.this);
                            } catch (final KeyStoreAES.KeyInvalidated e) {
                                Toast.makeText(PinSaveActivity.this, "Problem with key "
                                                + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
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
                    public boolean onEditorAction(final TextView v, final int actionId, @Nullable final KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                                actionId == EditorInfo.IME_ACTION_DONE ||
                                (event != null && event.getAction() == KeyEvent.ACTION_DOWN) &&
                                        event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                            if (event == null || !event.isShiftPressed()) {
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
                setResult(RESULT_CANCELED);
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
