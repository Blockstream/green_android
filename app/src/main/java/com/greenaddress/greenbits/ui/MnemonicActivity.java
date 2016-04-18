package com.greenaddress.greenbits.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.dd.CircularProgressButton;
import com.google.common.base.Joiner;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.greenaddress.greenapi.LoginData;
import com.greenaddress.greenbits.ConnectivityObservable;
import com.greenaddress.greenbits.GaService;

import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.annotation.Nullable;

import de.schildbach.wallet.ui.ScanActivity;


public class MnemonicActivity extends ActionBarActivity implements Observer {

    @NonNull private static final String TAG = MnemonicActivity.class.getSimpleName();

    private void showErrorCorrection(final String closeWord, final String badWord) {
        if (closeWord == null) return;
        final EditText mnemonicText = (EditText) findViewById(R.id.mnemonicText);
        final Snackbar snackbar = Snackbar
                .make(mnemonicText, getString(R.string.invalidWord, badWord, closeWord), Snackbar.LENGTH_LONG)
                .setAction("Correct", new View.OnClickListener() {
                    @Override
                    public void onClick(final View view) {
                        mnemonicText.setOnTouchListener(null);
                        final String mnemonicStr = mnemonicText.getText().toString()
                                .replace(badWord, closeWord);
                        mnemonicText.setText(mnemonicStr);
                        final int textLength = mnemonicStr.length();
                        mnemonicText.setSelection(textLength, textLength);

                        final int words = mnemonicStr.split(" ").length;
                        if (validateMnemonic(mnemonicStr) && (words == 24 || words == 27)) {
                            login();
                        }
                    }
                });

        final View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(Color.DKGRAY);
        final TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextColor(Color.WHITE);
        snackbar.show();
    }
    private boolean validateMnemonic(@NonNull final String mnemonic) {
        // FIXME: add support for different bip39 word lists like Japanese, Spanish, etc
        InputStream closable = null;
        try {
            closable = getAssets().open("bip39-wordlist.txt");
            new MnemonicCode(closable, null).check(Arrays.asList(mnemonic.split(" ")));
        } catch (@NonNull final IOException e) {
            // couldn't find mnemonics file
            throw new RuntimeException(e.getMessage());
        } catch (@NonNull final MnemonicException.MnemonicWordException e) {
            setWord(e.badWord);
            try {
                showErrorCorrection(MnemonicHelper.getClosestWord(e.badWord, this), e.badWord);
            } catch (@NonNull final IOException eGnore) {
                // ignore
            }
            return false;
        } catch (@NonNull final MnemonicException e) {
            return false;
        } finally {
            if (closable != null) {
                try {
                    closable.close();
                } catch (@NonNull final IOException e) {
                }
            }
        }
        return true;
    }


    private void login() {
        Futures.addCallback(getGAApp().onServiceAttached, new FutureCallback<Void>() {
            @Override
            public void onSuccess(final @Nullable Void result) {
                loginAfterServiceConnected();
            }

            @Override
            public void onFailure(@NonNull final Throwable t) {
                t.printStackTrace();
                Toast.makeText(MnemonicActivity.this, "Not connected, connection will resume automatically", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loginAfterServiceConnected() {
        final GaService gaService = getGAService();
        
        if (getGAApp().getConnectionObservable().getState() == ConnectivityObservable.State.LOGGEDIN) {
            Toast.makeText(MnemonicActivity.this, "You must first logout before signing in.", Toast.LENGTH_LONG).show();
            return;
        }

        if (getGAApp().getConnectionObservable().getState() != ConnectivityObservable.State.CONNECTED) {
            Toast.makeText(MnemonicActivity.this, "Not connected", Toast.LENGTH_LONG).show();
            return;
        }
        final EditText edit = (EditText) findViewById(R.id.mnemonicText);
        final CircularProgressButton okButton = (CircularProgressButton) findViewById(R.id.mnemonicOkButton);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);
            }
        });
        if (!validateMnemonic(edit.getText().toString())) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MnemonicActivity.this, "Invalid passphrase (has to be 24 or 27 words)", Toast.LENGTH_LONG).show();
                }
            });
            return;
        }

        okButton.setIndeterminateProgressMode(true);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //This larger function is somehow being called in another
                //non-main thread when cold-start NFC mnemonics login is
                // prompted, causing this to fail.
                okButton.setEnabled(false);
                okButton.setProgress(50);
            }
        });

        final AsyncFunction<Void, LoginData> connectToLogin = new AsyncFunction<Void, LoginData>() {
            @NonNull
            @Override
            public ListenableFuture<LoginData> apply(@Nullable final Void input) {
                if (edit.getText().toString().trim().split(" ").length == 27) {
                    // encrypted mnemonic
                    return Futures.transform(askForPassphrase(), new AsyncFunction<String, LoginData>() {
                        @Nullable
                        @Override
                        public ListenableFuture<LoginData> apply(final @Nullable String passphrase) {
                            try {
                                final byte[] entropy = gaService.getMnemonicCode().toEntropy(Arrays.asList(edit.getText().toString().trim().split(" ")));
                                final String normalizedPassphrase = Normalizer.normalize(passphrase, Normalizer.Form.NFC);
                                final byte[] decrypted = MnemonicHelper.decryptMnemonic(entropy, normalizedPassphrase);
                                return gaService.login(Joiner.on(" ").join(gaService.getMnemonicCode().toMnemonic(decrypted)));
                            } catch (@NonNull final IOException | GeneralSecurityException | MnemonicException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
                } else {
                    return gaService.login(edit.getText().toString().trim());
                }
            }
        };

        final ListenableFuture<LoginData> loginFuture = Futures.transform(gaService.onConnected, connectToLogin, gaService.es);

        Futures.addCallback(loginFuture, new FutureCallback<LoginData>() {
            @Override
            public void onSuccess(@Nullable final LoginData result) {
                if (getCallingActivity() == null) {
                    final Intent pinSaveActivity = new Intent(MnemonicActivity.this, PinSaveActivity.class);
                    pinSaveActivity.putExtra("com.greenaddress.greenbits.NewPinMnemonic", gaService.getMnemonics());
                    startActivity(pinSaveActivity);
                } else {
                    setResult(RESULT_OK);
                }
                MnemonicActivity.this.finish();
            }

            @Override
            public void onFailure(@NonNull final Throwable t) {
                final boolean accountDoesntExist = t instanceof ClassCastException;
                final String message = accountDoesntExist ? "Account doesn't exist" : "Login failed";
                t.printStackTrace();
                MnemonicActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MnemonicActivity.this, message, Toast.LENGTH_LONG).show();
                        okButton.setProgress(0);
                        okButton.setEnabled(true);
                    }
                });
            }
        }, gaService.es);
    }



    @NonNull
    private ListenableFuture<String> askForPassphrase() {
        final SettableFuture<String> passphraseFuture = SettableFuture.create();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final View inflatedLayout = getLayoutInflater().inflate(R.layout.dialog_passphrase, null, false);
                final EditText passphraseValue = (EditText) inflatedLayout.findViewById(R.id.passphraseValue);
                final MaterialDialog dialog = new MaterialDialog.Builder(MnemonicActivity.this)
                        .title("Encryption passphrase")
                        .customView(inflatedLayout, true)
                        .positiveText("OK")
                        .negativeText("CANCEL")
                        .positiveColorRes(R.color.accent)
                        .negativeColorRes(R.color.accent)
                        .titleColorRes(R.color.white)
                        .contentColorRes(android.R.color.white)
                        .theme(Theme.DARK)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(final @NonNull MaterialDialog dialog, final @NonNull DialogAction which) {
                                passphraseFuture.set(passphraseValue.getText().toString());
                            }
                        })
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull final MaterialDialog dialog, @NonNull final DialogAction which) {
                                final CircularProgressButton okButton = (CircularProgressButton) findViewById(R.id.mnemonicOkButton);
                                okButton.setProgress(0);
                                okButton.setEnabled(true);
                            }
                        })
                        .build();
                // (FIXME not sure if there's any smaller subset of these 3 calls below which works too)
                passphraseValue.requestFocus();
                dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                dialog.show();
            }
        });
        return passphraseFuture;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, getIntent().getType() + "" + getIntent());
        setContentView(R.layout.activity_mnemonic);
        final CircularProgressButton okButton = (CircularProgressButton) findViewById(R.id.mnemonicOkButton);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                MnemonicActivity.this.login();
            }
        });


        final TextView scanIcon = (TextView) findViewById(R.id.mnemonicScanIcon);
        scanIcon.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(final View view) {
                                            //New Marshmallow permissions paradigm
                                            final String[] perms = {"android.permission.CAMERA"};
                                            if (Build.VERSION.SDK_INT>Build.VERSION_CODES.LOLLIPOP_MR1 &&
                                                    checkSelfPermission(perms[0]) != PackageManager.PERMISSION_GRANTED) {
                                                final int permsRequestCode = 150;
                                                requestPermissions(perms, permsRequestCode);
                                            } else {
                                                final Intent scanner = new Intent(MnemonicActivity.this, ScanActivity.class);
                                                startActivityForResult(scanner, 0);
                                            }
                                        }
                                    }
        );

        final EditText edit = (EditText) findViewById(R.id.mnemonicText);

        edit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(final TextView v, final int actionId, final KeyEvent event) {
                if(event != null && KeyEvent.KEYCODE_ENTER == event.getKeyCode()) {
                    return true;
                }
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    login();
                    return true;
                }
                return false;
            }
        });

        edit.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(final Editable s) {
                final String mnemonic = s.toString();
                if (mnemonic.startsWith(" ")) {
                    s.replace(0, 1, "");
                    return;
                }
                final int space_idx = mnemonic.lastIndexOf("  ");
                if (space_idx != -1) {
                    try {
                        s.replace(space_idx, 2, " ");
                    } catch (final IndexOutOfBoundsException ignore) {
                        // seems caused by backspace on a physical keyboard via emulator
                        // ideally we handle this better but this seems to work
                    }
                    return;
                }
                final String[] split = mnemonic.split(" ");
                final boolean endsWithSpace = mnemonic.endsWith(" ");
                final int lastElement = split.length - 1;
                for (int i = 0; i < split.length; ++i) {
                    final String word = split[i];
                    try {
                        // check for equality
                        // not last or last but postponed by a space
                        // otherwise just that it's the start of a word
                        if (MnemonicHelper.isInvalidWord(word, MnemonicActivity.this,
                                !(i == lastElement) || endsWithSpace)) {
                            if (spans != null && word.equals(spans.word)) {
                                return;
                            }
                            setWord(word);
                            return;
                        }
                    } catch (final IOException e) {
                        e.printStackTrace();
                    }
                }
                edit.setOnTouchListener(null);
                final Spans copy = spans;
                spans = null;
                if (copy != null) {
                    for (final Object span : copy.spans) {
                        s.removeSpan(span);
                    }
                }
            }
            public void beforeTextChanged(final CharSequence s, final int start,
                                          final int count, final int after){}
            public void onTextChanged(final CharSequence s, final int start,
                                      final int before, final int count) {
            }
        });

        NFCIntentMnemonicLogin();
    }

    private void NFCIntentMnemonicLogin() {
        final EditText edit = (EditText) findViewById(R.id.mnemonicText);

        final Intent intent = getIntent();
        if (intent != null && NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            edit.setTextColor(Color.WHITE);
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
                    final GaService gaService = getGAService();
                    edit.setText(mnemonics);

                    if (gaService != null && gaService.onConnected != null && !mnemonics.equals(gaService.getMnemonics())) {
                        //Auxillary Future to make sure we are connected.
                        Futures.addCallback(gaService.triggerOnFullyConnected, new FutureCallback<Void>() {
                            @Override
                            public void onSuccess(@Nullable final Void result) {
                                login();
                            }

                            @Override
                            public void onFailure(@NonNull final Throwable t) {

                            }
                        });
                    }

                } catch (@NonNull final IOException | MnemonicException e) {
                    e.printStackTrace();
                } finally {
                    if (closable != null) {
                        try {
                            closable.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else if (intent.getType().equals("x-ga/en")) {
                // encrypted nfc
                final Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
                final byte[] array = ((NdefMessage) rawMessages[0]).getRecords()[0].getPayload();
                Futures.addCallback(askForPassphrase(), new FutureCallback<String>() {
                    @Override
                    public void onSuccess(final @Nullable String passphrase) {
                        try {
                            final byte[] decrypted = MnemonicHelper.decryptMnemonic(array, passphrase);
                            final GaService gaService = getGAService();
                            final String mnemonics = Joiner.on(" ").join(new MnemonicCode().toMnemonic(decrypted));
                            edit.setText(mnemonics);
                            if (gaService != null && gaService.onConnected != null && !mnemonics.equals(gaService.getMnemonics())) {
                                Futures.addCallback(gaService.onConnected, new FutureCallback<Void>() {
                                    @Override
                                    public void onSuccess(@Nullable final Void result) {
                                        login();
                                    }

                                    @Override
                                    public void onFailure(@NonNull final Throwable t) {

                                    }
                                });
                            }
                        } catch (@NonNull final GeneralSecurityException | IOException | MnemonicException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull final Throwable t) {

                    }
                });
            }

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getGAApp().getConnectionObservable().addObserver(this);

        final ConnectivityObservable.State state = getGAApp().getConnectionObservable().getState();
        if (state.equals(ConnectivityObservable.State.LOGGEDIN)) {
            // already logged in, could be from different app via intent
            final Intent mainActivity = new Intent(MnemonicActivity.this, TabbedMainActivity.class);
            startActivity(mainActivity);
            finish();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        getGAApp().getConnectionObservable().deleteObserver(this);
    }

    private Spans spans;

    private void setWord(final String badWord) {

        final EditText mnemonicText = (EditText) findViewById(R.id.mnemonicText);

        mnemonicText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(final View view, final MotionEvent motionEvent) {
                try {
                    showErrorCorrection(MnemonicHelper.getClosestWord(badWord, MnemonicActivity.this), badWord);
                } catch (@NonNull final IOException eGnore) {
                    // ignore
                }
                return false;
            }
        });

        final Spannable spannable = mnemonicText.getText();
        final String mnemonics = spannable.toString();

        int start = 0;
        for (final String word: mnemonics.split(" ")) {
            if (word.equals(badWord)) break;
            start += word.length() + 1;
        }

        final int end = start + badWord.length();

        if (spans != null) {
            for (final Object o: spans.spans) {
                spannable.removeSpan(o);
            }
        }

        spans = new Spans(badWord);
        for (final Object s: spans.spans) {
            spannable.setSpan(s, start, end, 0);
        }
    }

    class Spans {
        final String word;
        final List<Object> spans = new ArrayList<>(4);
        Spans(final String word) {
            this.word = word;
            spans.add(new StyleSpan(Typeface.BOLD));
            spans.add(new StyleSpan(Typeface.ITALIC));
            spans.add(new UnderlineSpan());
            spans.add(new ForegroundColorSpan(Color.RED));
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, @android.support.annotation.Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
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
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return item.getItemId() == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    @Override
    public void update(final Observable observable, final Object data) {

    }

    @Override
    public void onRequestPermissionsResult(final int permsRequestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        switch (permsRequestCode) {

            case 150:

                final boolean cameraPermissionGranted = grantResults[0]== PackageManager.PERMISSION_GRANTED;

                if (cameraPermissionGranted) {
                    final Intent scanner = new Intent(MnemonicActivity.this, ScanActivity.class);
                    startActivityForResult(scanner, 0);
                }
                else {
                    Toast.makeText(getApplicationContext(), "Please enable camera permissions to use scan functionality.", Toast.LENGTH_SHORT).show();
                }
        }
    }
}
