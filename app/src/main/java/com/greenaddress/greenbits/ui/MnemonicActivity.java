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

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.blockstream.libwally.Wally;
import com.dd.CircularProgressButton;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.greenaddress.greenapi.CryptoHelper;
import com.greenaddress.greenapi.LoginData;
import com.greenaddress.greenbits.ConnectivityObservable;
import com.greenaddress.greenbits.GaService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import de.schildbach.wallet.ui.ScanActivity;


public class MnemonicActivity extends GaActivity {

    private static final int PINSAVE = 1337;
    private static final int QRSCANNER = 1338;
    private static final int CAMERA_PERMISSION = 150;


    @NonNull private static final String TAG = MnemonicActivity.class.getSimpleName();

    private Set<String> words = new HashSet<>(Wally.BIP39_WORDLIST_LEN);

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
        try {
            Wally.bip39_mnemonic_validate(Wally.bip39_get_wordlist("en"), mnemonic);
            return true;
        } catch (final IllegalArgumentException e) {
            for (final String w : mnemonic.split(" ")) {
                if (!words.contains(w)) {
                    setWord(w);
                    showErrorCorrection(MnemonicHelper.getClosestWord(words.toArray(new String[Wally.BIP39_WORDLIST_LEN]), w), w);

                    break;
                }
            }
            return false;
        }
    }

    private void login() {
        final GaService service = mService;
        
        final ConnectivityObservable.ConnectionState cs = getGAApp().getConnectionObservable().getState();
        if (cs.mState == ConnectivityObservable.State.LOGGEDIN) {
            toast(R.string.err_mnemonic_activity_logout_required);
            return;
        }

        if (cs.mState != ConnectivityObservable.State.CONNECTED) {
            toast(R.string.err_send_not_connected_will_resume);
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
            MnemonicActivity.this.toast(R.string.err_mnemonic_activity_invalid_mnemonic);
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
                final String mnemonics = edit.getText().toString().trim();
                if (mnemonics.split(" ").length == 27) {
                    // encrypted mnemonic
                    return Futures.transform(askForPassphrase(), new AsyncFunction<String, LoginData>() {
                        @Nullable
                        @Override
                        public ListenableFuture<LoginData> apply(final @Nullable String passphrase) {
                            return service.login(
                                    CryptoHelper.encrypted_mnemonic_to_mnemonic(mnemonics, passphrase));

                        }
                    });
                } else {
                    return service.login(edit.getText().toString().trim());
                }
            }
        };

        final ListenableFuture<LoginData> loginFuture = Futures.transform(service.onConnected, connectToLogin, service.es);

        Futures.addCallback(loginFuture, new FutureCallback<LoginData>() {
            @Override
            public void onSuccess(@Nullable final LoginData result) {
                if (getCallingActivity() == null) {
                    final Intent pinSaveActivity = new Intent(MnemonicActivity.this, PinSaveActivity.class);
                    pinSaveActivity.putExtra("com.greenaddress.greenbits.NewPinMnemonic", service.getMnemonics());
                    startActivityForResult(pinSaveActivity, PINSAVE);
                } else {
                    setResult(RESULT_OK);
                    MnemonicActivity.this.finish();
                }
            }

            @Override
            public void onFailure(@NonNull final Throwable t) {
                final boolean accountDoesntExist = t instanceof ClassCastException;
                final String message = accountDoesntExist ? "Account doesn't exist" : "Login failed";
                t.printStackTrace();
                MnemonicActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MnemonicActivity.this.toast(message);
                        okButton.setProgress(0);
                        okButton.setEnabled(true);
                    }
                });
            }
        }, service.es);
    }



    @NonNull
    private ListenableFuture<String> askForPassphrase() {
        final SettableFuture<String> passphraseFuture = SettableFuture.create();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final View inflatedLayout = getLayoutInflater().inflate(R.layout.dialog_passphrase, null, false);
                final EditText passphraseValue = (EditText) inflatedLayout.findViewById(R.id.passphraseValue);
                passphraseValue.requestFocus();
                final MaterialDialog dialog = Popup(MnemonicActivity.this, "Encryption passphrase")
                        .customView(inflatedLayout, true)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(final @NonNull MaterialDialog dlg, final @NonNull DialogAction which) {
                                passphraseFuture.set(passphraseValue.getText().toString());
                            }
                        })
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull final MaterialDialog dlg, @NonNull final DialogAction which) {
                                final CircularProgressButton okButton = (CircularProgressButton) findViewById(R.id.mnemonicOkButton);
                                okButton.setProgress(0);
                                okButton.setEnabled(true);
                            }
                        }).build();
                // (FIXME not sure if there's any smaller subset of these 3 calls below which works too)
                dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                dialog.show();
            }
        });
        return passphraseFuture;
    }

    protected int getMainViewId() { return R.layout.activity_mnemonic; }

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState,
                                       final ConnectivityObservable.ConnectionState cs) {
        Log.i(TAG, getIntent().getType() + "" + getIntent());

        mapClick(R.id.mnemonicOkButton, new View.OnClickListener() {
            public void onClick(final View v) {
                MnemonicActivity.this.login();
            }
        });
        mapClick(R.id.mnemonicScanIcon, new View.OnClickListener() {
                                        public void onClick(final View view) {
                                            //New Marshmallow permissions paradigm
                                            final String[] perms = {"android.permission.CAMERA"};
                                            if (Build.VERSION.SDK_INT>Build.VERSION_CODES.LOLLIPOP_MR1 &&
                                                    checkSelfPermission(perms[0]) != PackageManager.PERMISSION_GRANTED) {
                                                requestPermissions(perms, CAMERA_PERMISSION);
                                            } else {
                                                final Intent scanner = new Intent(MnemonicActivity.this, ScanActivity.class);
                                                startActivityForResult(scanner, QRSCANNER);
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
                    // check for equality
                    // not last or last but postponed by a space
                    // otherwise just that it's the start of a word
                    if (MnemonicHelper.isInvalidWord(
                            words.toArray(new String[Wally.BIP39_WORDLIST_LEN]), word,
                            !(i == lastElement) || endsWithSpace)) {
                        if (spans != null && word.equals(spans.word)) {
                            return;
                        }
                        setWord(word);
                        return;
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
        final Object en = Wally.bip39_get_wordlist("en");
        for (int i = 0; i < Wally.BIP39_WORDLIST_LEN; ++i)
            words.add(Wally.bip39_get_word(en, i));

        NFCIntentMnemonicLogin();
    }

    private void NFCIntentMnemonicLogin() {
        final GaService service = mService;

        final EditText edit = (EditText) findViewById(R.id.mnemonicText);

        final Intent intent = getIntent();
        if (intent != null && NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            edit.setTextColor(Color.WHITE);
            if (intent.getType().equals("x-gait/mnc")) {
                // not encrypted nfc
                final Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

                final String mnemonics = CryptoHelper.mnemonic_from_bytes(
                        ((NdefMessage) rawMessages[0]).getRecords()[0].getPayload());

                edit.setText(mnemonics);

                if (service.onConnected != null && !mnemonics.equals(service.getMnemonics())) {
                    //Auxillary Future to make sure we are connected.
                    CB.after(service.onConnected, new CB.NoOp<Void>() {
                        @Override
                        public void onSuccess(final Void result) {
                            login();
                        }
                    });
                }
            } else if (intent.getType().equals("x-ga/en")) {
                // encrypted nfc
                final Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
                final byte[] array = ((NdefMessage) rawMessages[0]).getRecords()[0].getPayload();
                CB.after(askForPassphrase(), new CB.NoOp<String>() {
                    @Override
                    public void onSuccess(final String passphrase) {
                        final String mnemonics = CryptoHelper.encrypted_mnemonic_to_mnemonic(array, passphrase);
                        edit.setText(mnemonics);
                        if (service.onConnected != null && !mnemonics.equals(service.getMnemonics())) {
                            CB.after(service.onConnected, new CB.NoOp<Void>() {
                                @Override
                                public void onSuccess(final Void result) {
                                    login();
                                }
                            });
                        }
                    }
                });
            }

        }
    }

    @Override
    protected void onResumeWithService(final ConnectivityObservable.ConnectionState cs) {
        if (cs.mState.equals(ConnectivityObservable.State.LOGGEDIN)) {
            // already logged in, could be from different app via intent
            final Intent mainActivity = new Intent(MnemonicActivity.this, TabbedMainActivity.class);
            startActivity(mainActivity);
            finish();
        }
    }

    private Spans spans;

    private void setWord(final String badWord) {

        final EditText mnemonicText = (EditText) findViewById(R.id.mnemonicText);

        mnemonicText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(final View view, final MotionEvent motionEvent) {
                showErrorCorrection(MnemonicHelper.getClosestWord(words.toArray(new String[Wally.BIP39_WORDLIST_LEN]), badWord), badWord);
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
        switch (requestCode) {
            case PINSAVE:
                final Intent tabbedMainActivity = new Intent(MnemonicActivity.this, TabbedMainActivity.class);
                startActivity(tabbedMainActivity);
                finish();
                break;
            case QRSCANNER:
                final EditText edit = (EditText) findViewById(R.id.mnemonicText);
                if (data != null && data.getStringExtra("com.greenaddress.greenbits.QrText") != null) {
                    edit.setText(data.getStringExtra("com.greenaddress.greenbits.QrText"));
                    login();
                }
                break;
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
    public void onRequestPermissionsResult(final int permsRequestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        switch (permsRequestCode) {

            case CAMERA_PERMISSION:

                final boolean cameraPermissionGranted = grantResults[0]== PackageManager.PERMISSION_GRANTED;

                if (cameraPermissionGranted) {
                    final Intent scanner = new Intent(MnemonicActivity.this, ScanActivity.class);
                    startActivityForResult(scanner, QRSCANNER);
                }
                else
                    shortToast(R.string.err_qrscan_requires_camera_permissions);
        }
    }
}
