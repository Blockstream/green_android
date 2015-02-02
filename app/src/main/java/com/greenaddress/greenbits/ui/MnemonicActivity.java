package com.greenaddress.greenbits.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.ActionBarActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.dd.CircularProgressButton;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.greenaddress.greenapi.LoginData;
import com.greenaddress.greenbits.ConnectivityObservable;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.GreenAddressApplication;

import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.annotation.Nullable;

import de.schildbach.wallet.ui.ScanActivity;


public class MnemonicActivity extends ActionBarActivity implements Observer {

    private static int countSubStr(final String sub, final String s) {
        int c = 0;
        for (int l = s.indexOf(sub); l != -1;
             l = s.indexOf(sub, l + sub.length())) {
            ++c;
        }
        return c;
    }

    private static int levenshteinDistance(final String inputA, final String inputB) {
        final String strA = inputA.toLowerCase();
        final String strB = inputB.toLowerCase();
        final int[] c = new int[strB.length() + 1];
        for (int i = 0; i < c.length; ++i) {
            c[i] = i;
        }
        for (int i = 1; i <= strA.length(); ++i) {
            c[0] = i;
            int n = i - 1;
            for (int j = 1; j <= strB.length(); ++j) {
                final int cj = Math.min(1 + Math.min(c[j], c[j - 1]), strA.charAt(i - 1) == strB.charAt(j - 1) ? n : n + 1);
                n = c[j];
                c[j] = cj;
            }
        }
        return c[strB.length()];
    }

    private String getClosestWord(final String word) throws IOException {
        InputStream is = null;
        is = getAssets().open("bip39-wordlist.txt");
        final List<String> words = new ArrayList<>();

        String line;

        try {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            while ((line = reader.readLine()) != null) {
                words.add(line);
            }
        } finally {
            is.close();
        }

        final List<Integer> scores = new ArrayList<>();
        for (final String w : words) {
            scores.add(new Integer(Integer.MAX_VALUE));
        }

        for (int i = 0; i < words.size(); ++i) {
            scores.set(i, levenshteinDistance(word, words.get(i)));
        }

        return words.get(scores.indexOf(Collections.min(scores)));
    }

    private boolean validateMnemonic(final String mnemonic) {
        // FIXME: add support for BIP38'ed mnemonics
        // FIXME: add support for different bip39 word lists like Japanese, Spanish, etc
        InputStream closable = null;
        try {
            closable = getAssets().open("bip39-wordlist.txt");
            new MnemonicCode(closable, null).check(Arrays.asList(mnemonic.split(" ")));
        } catch (final IOException e) {
            Toast.makeText(MnemonicActivity.this, "Can't find resources file bip39-wordlist.txt, please contact support.", Toast.LENGTH_LONG).show();
            return false;
        } catch (final MnemonicException.MnemonicWordException e) {
            // show red only if there's a single match as we don't know the position of the failure
            if (countSubStr(e.badWord, mnemonic) == 1) {
                final int start = mnemonic.indexOf(e.badWord);
                final int end = start + e.badWord.length();
                final Spannable spannable = new SpannableString(mnemonic);
                spannable.setSpan(new StyleSpan(Typeface.BOLD), start, end, 0);
                spannable.setSpan(new StyleSpan(Typeface.ITALIC), start, end, 0);
                spannable.setSpan(new UnderlineSpan(), start, end, 0);
                spannable.setSpan(new ForegroundColorSpan(Color.RED), start, end, 0);
                ((EditText) findViewById(R.id.mnemonicText)).setText(spannable);
            }
            String closeworld = null;
            try {
                closeworld = getClosestWord(e.badWord);
            } catch (final IOException eGnore) {
                // ignore
            }
            if (closeworld == null) {
                Toast.makeText(MnemonicActivity.this, "'" + e.badWord + "'" + " is not a valid word", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(MnemonicActivity.this, "'" + e.badWord + "'" + " is not a valid word, did you mean '" + closeworld + "'?", Toast.LENGTH_LONG).show();
            }
            return false;
        } catch (final MnemonicException e) {
            Toast.makeText(MnemonicActivity.this, "Invalid passphrase (has to be 24 or 27 words)", Toast.LENGTH_LONG).show();
            return false;
        } finally {
            if (closable != null) {
                try {
                    closable.close();
                } catch (final IOException e) {
                }
            }
        }
        return true;
    }


    private void login() {
        Futures.addCallback(((GreenAddressApplication) getApplication()).onServiceConnected, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                loginAfterServiceConnected();
            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
                Toast.makeText(MnemonicActivity.this, "Not connected, connection will resume automatically", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loginAfterServiceConnected() {
        final GaService gaService = ((GreenAddressApplication) getApplication()).gaService;
        if (((GreenAddressApplication) getApplication()).getConnectionObservable().getState() != ConnectivityObservable.State.CONNECTED) {
            Toast.makeText(MnemonicActivity.this, "Not connected", Toast.LENGTH_LONG).show();
            return;
        }
        final EditText edit = (EditText) findViewById(R.id.mnemonicText);
        final CircularProgressButton okButton = (CircularProgressButton) findViewById(R.id.mnemonicOkButton);


        if (!validateMnemonic(edit.getText().toString())) {
            return;
        }

        okButton.setIndeterminateProgressMode(true);
        okButton.setProgress(50);

        final AsyncFunction<Void, LoginData> connectToLogin = new AsyncFunction<Void, LoginData>() {
            @Override
            public ListenableFuture<LoginData> apply(final Void input) {
                return gaService.login(edit.getText().toString().trim());
            }
        };

        final ListenableFuture<LoginData> loginFuture = Futures.transform(gaService.onConnected, connectToLogin, gaService.es);

        Futures.addCallback(loginFuture, new FutureCallback<LoginData>() {
            @Override
            public void onSuccess(@Nullable final LoginData result) {
                if (getCallingActivity() == null) {
                    final Intent pinSaveActivity = new Intent(MnemonicActivity.this, PinSaveActivity.class);
                    pinSaveActivity.putExtra("com.greenaddress.greenbits.NewPinMnemonic", edit.getText().toString());
                    startActivity(pinSaveActivity);
                } else {
                    setResult(RESULT_OK);
                }
                MnemonicActivity.this.finish();
            }

            @Override
            public void onFailure(final Throwable t) {
                final boolean accountDoesntExist = t instanceof ClassCastException;
                final String message = accountDoesntExist ? "Account doesn't exist" : "Login failed";

                MnemonicActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MnemonicActivity.this, message, Toast.LENGTH_LONG).show();
                        okButton.setProgress(0);

                    }
                });
            }
        }, gaService.es);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("MnemonicActivity", "" + getIntent());
        setContentView(R.layout.activity_mnemonic);
        final CircularProgressButton okButton = (CircularProgressButton) findViewById(R.id.mnemonicOkButton);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                MnemonicActivity.this.login();
            }
        });
        final EditText edit = (EditText) findViewById(R.id.mnemonicText);

        edit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(final TextView v, final int actionId, final KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    login();
                    return true;
                }
                return false;
            }
        });

        final TextView scanIcon = (TextView) findViewById(R.id.mnemonicScanIcon);
        scanIcon.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(final View view) {
                                            final Intent scanner = new Intent(MnemonicActivity.this, ScanActivity.class);
                                            startActivityForResult(scanner, 0);
                                        }
                                    }
        );

        final Intent intent = getIntent();
        if (intent != null && NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {

            if (intent.getType().equals("x-gait/mnc")) {
                // not encrypted nfc
                InputStream closable = null;
                try {
                    closable = getAssets().open("bip39-wordlist.txt");
                    final Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
                    final byte[] array = ((NdefMessage) rawMessages[0]).getRecords()[0].getPayload();
                    final String mnemonics = TextUtils.join(" ",
                            new MnemonicCode(closable, null)
                                    .toMnemonic(array));
                    final GaService gaService = ((GreenAddressApplication) getApplication()).gaService;
                    edit.setText(mnemonics);

                    if (gaService != null && gaService.onConnected != null && !mnemonics.equals(gaService.getMnemonics())) {

                        Futures.addCallback(gaService.onConnected, new FutureCallback<Void>() {
                            @Override
                            public void onSuccess(@Nullable final Void result) {
                                login();
                            }

                            @Override
                            public void onFailure(final Throwable t) {

                            }
                        });
                    }


                } catch (final IOException e) {

                } catch (final MnemonicException e) {

                } finally {
                    if (closable != null) {
                        try {
                            closable.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ((GreenAddressApplication) getApplication()).getConnectionObservable().addObserver(this);

        ConnectivityObservable.State state = ((GreenAddressApplication) getApplication()).getConnectionObservable().getState();
        if (state.equals(ConnectivityObservable.State.LOGGEDIN)) {
            // already logged in, could be from different app via intent
            final Intent mainActivity = new Intent(MnemonicActivity.this, TabbedMainActivity.class);
            startActivity(mainActivity);
            finish();
            return;
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        ((GreenAddressApplication) getApplication()).getConnectionObservable().deleteObserver(this);
    }


    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i("MnemonicActivity", "" + data);
        final EditText edit = (EditText) findViewById(R.id.mnemonicText);
        if (data != null && data.getStringExtra("com.greenaddress.greenbits.QrText") != null) {
            edit.setText(data.getStringExtra("com.greenaddress.greenbits.QrText"));
            login();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // getMenuInflater().inflate(R.menu.mnemonic, menu);
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
    public void update(final Observable observable, final Object data) {

    }
}
