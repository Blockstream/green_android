package com.greenaddress.greenbits.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.dd.CircularProgressButton;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.greenaddress.greenapi.PinData;

import org.bitcoinj.crypto.MnemonicCode;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.annotation.Nullable;


public class PinSaveActivity extends ActionBarActivity implements Observer {

    private void setPin(final CircularProgressButton pinSaveButton, final EditText pinText) {
        if (pinText.getText().toString().length() < 4) {
            Toast.makeText(PinSaveActivity.this, "PIN has to be between 4 and 15 digits", Toast.LENGTH_SHORT).show();
            return;
        }
        final String mnemonic_str = getIntent().getStringExtra("com.greenaddress.greenbits.NewPinMnemonic");
        final List<String> mnemonic = java.util.Arrays.asList(mnemonic_str.split(" "));
        final Button pinSkipButton = (Button) findViewById(R.id.pinSkipButton);

        pinSaveButton.setIndeterminateProgressMode(true);
        pinSaveButton.setProgress(50);
        pinSkipButton.setVisibility(View.GONE);
        Futures.addCallback(getGAService().setPin(MnemonicCode.toSeed(mnemonic, ""), mnemonic_str,
                        pinText.getText().toString(), "default"),
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
                    public void onFailure(final Throwable t) {
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

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_save);

        final EditText pinSaveText = (EditText) findViewById(R.id.pinSaveText);

        final CircularProgressButton pinSaveButton = (CircularProgressButton) findViewById(R.id.pinSaveButton);
        final Button pinSkipButton = (Button) findViewById(R.id.pinSkipButton);

        pinSaveText.setOnEditorActionListener(
                new EditText.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(final TextView v, final int actionId, final KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                                actionId == EditorInfo.IME_ACTION_DONE ||
                                event.getAction() == KeyEvent.ACTION_DOWN &&
                                        event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                            if (event == null || !event.isShiftPressed()) {
                                // the user is done typing.
                                setPin(pinSaveButton, pinSaveText);
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
                setPin(pinSaveButton, pinSaveText);
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
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void update(Observable observable, Object data) {

    }
}
