package com.greenaddress.greenbits.ui;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.Snackbar;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StrikethroughSpan;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.MultiAutoCompleteTextView;

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

import de.schildbach.wallet.ui.ScanActivity;


public class MnemonicActivity extends LoginActivity implements View.OnClickListener {

    private static final String TAG = MnemonicActivity.class.getSimpleName();

    private static final int PINSAVE = 1337;
    private static final int QRSCANNER = 1338;
    private static final int CAMERA_PERMISSION = 150;

    private MultiAutoCompleteTextView mMnemonicText;
    private CircularProgressButton mOkButton;
    private TextView mScanButton;

    final private MultiAutoCompleteTextView.Tokenizer mTokenizer = new MultiAutoCompleteTextView.Tokenizer() {
        private boolean isspace(final CharSequence t, final int pos) {
            return Character.isWhitespace(t.charAt(pos));
        }

        public int findTokenStart(final CharSequence t, int cursor) {
            final int end = cursor;
            while (cursor > 0 && !isspace(t, cursor - 1))
                --cursor;
            while (cursor < end && isspace(t, cursor))
                ++cursor;
            return cursor;
        }

        public int findTokenEnd(final CharSequence t, int cursor) {
            final int end = t.length();
            while (cursor < end && !isspace(t, cursor))
                ++cursor;
            return cursor;
        }

        public CharSequence terminateToken(final CharSequence t) {
            int cursor = t.length();

            while (cursor > 0 && isspace(t, cursor - 1))
                cursor--;
            if (cursor > 0 && isspace(t, cursor - 1))
                return t;
            if (t instanceof Spanned) {
                final SpannableString sp = new SpannableString(t + " ");
                TextUtils.copySpansFrom((Spanned) t, 0, t.length(), Object.class, sp, 0);
                return sp;
            }
            return t + " ";
        }
    };

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {
        Log.i(TAG, getIntent().getType() + ' ' + getIntent());

        mMnemonicText = UI.find(this, R.id.mnemonicText);
        mOkButton = UI.find(this,R.id.mnemonicOkButton);
        mScanButton = UI.find(this,R.id.mnemonicScanIcon);

        mOkButton.setIndeterminateProgressMode(true);
        mOkButton.setOnClickListener(this);

        final boolean haveCamera = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
        UI.showIf(haveCamera, mScanButton);
        if (haveCamera)
            mScanButton.setOnClickListener(this);

        final ArrayAdapter<String> adapter;
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, MnemonicHelper.mWordsArray);
        mMnemonicText.setAdapter(adapter);
        mMnemonicText.setThreshold(1);
        mMnemonicText.setTokenizer(mTokenizer);
        mMnemonicText.addTextChangedListener(new UI.TextWatcher() {
            @Override
            public void onTextChanged(final CharSequence t, final int start,
                                      final int before, final int count) {
                markInvalidWords();
            }
        });

        NFCIntentMnemonicLogin();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        UI.unmapClick(mOkButton);
        UI.unmapClick(mScanButton);
    }

    private void markInvalidWords() {
        final Editable e = mMnemonicText.getText();
        for (final StrikethroughSpan s : e.getSpans(0, e.length(), StrikethroughSpan.class))
            e.removeSpan(s);

        int start = 0;
        for (final String word : e.toString().split(" ")) {
            final int end = start + word.length();
            if (!MnemonicHelper.mWords.contains(word))
                e.setSpan(new StrikethroughSpan(), start, end, 0);
            start = end + 1;
        }
    }

    private void promptToFixInvalidWord(final String badWord, final int start, final int end) {
        // FIXME: Show a list of closest words instead of just one
        final String closeWord = MnemonicHelper.getClosestWord(MnemonicHelper.mWordsArray, badWord);
        final Snackbar snackbar = Snackbar
            .make(mMnemonicText, getString(R.string.invalidWord, badWord, closeWord), Snackbar.LENGTH_LONG)
            .setAction("Correct", new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    // FIXME: Use start/end to replace and try logging in again
                    setMnemonic(getMnemonic().replace(badWord, closeWord));
                }
            });
        final View v = snackbar.getView();
        v.setBackgroundColor(Color.DKGRAY);
        final TextView textView = UI.find(v, android.support.design.R.id.snackbar_text);
        textView.setTextColor(Color.WHITE);
        snackbar.show();
    }

    protected int getMainViewId() { return R.layout.activity_mnemonic; }

    private String getMnemonic() {
        return UI.getText(mMnemonicText).replaceAll("\\s+", " ").trim();
    }

    private void setMnemonic(final String mnemonic) {
        if (!UI.getText(mMnemonicText).equals(mnemonic))
            mMnemonicText.setText(mnemonic);
        mMnemonicText.setSelection(mnemonic.length(), mnemonic.length());
    }

    private void enableLogin() {
        mOkButton.setProgress(0);
        mMnemonicText.setEnabled(true);
    }

    private void doLogin() {
        final String mnemonic = getMnemonic();
        setMnemonic(mnemonic); // Trim mnemonic when OK pressed

        if (mOkButton.getProgress() != 0)
            return;

        if (mService.isLoggedIn()) {
            toast(R.string.err_mnemonic_activity_logout_required);
            return;
        }

        if (!mService.isConnected()) {
            toast(R.string.err_send_not_connected_will_resume);
            return;
        }

        final String words[] = mnemonic.split(" ");
        if (words.length != 24 && words.length != 27) {
            toast(R.string.err_mnemonic_activity_invalid_mnemonic);
            return;
        }

        int start = 0;
        for (final String word : words) {
            final int end = start + word.length();
            if (!MnemonicHelper.mWords.contains(word)) {
                promptToFixInvalidWord(word, start, end);
                return;
            }
            start = end + 1;
        }

        try {
            Wally.bip39_mnemonic_validate(Wally.bip39_get_wordlist("en"), mnemonic);
        } catch (final IllegalArgumentException e) {
            toast(R.string.err_mnemonic_activity_invalid_mnemonic); // FIXME: Use different error message
            return;
        }

        mOkButton.setProgress(50);
        mMnemonicText.setEnabled(false);
        hideKeyboardFrom(mMnemonicText);

        final AsyncFunction<Void, LoginData> connectToLogin = new AsyncFunction<Void, LoginData>() {
            @Override
            public ListenableFuture<LoginData> apply(final Void input) {
                if (words.length != 27)
                    return mService.login(mnemonic);

                // Encrypted mnemonic
                return Futures.transform(askForPassphrase(), new AsyncFunction<String, LoginData>() {
                    @Override
                    public ListenableFuture<LoginData> apply(final String passphrase) {
                        return mService.login(CryptoHelper.decrypt_mnemonic(mnemonic, passphrase));
                    }
                });
            }
        };

        final ListenableFuture<LoginData> loginFuture;
        loginFuture = Futures.transform(mService.onConnected, connectToLogin, mService.getExecutor());

        Futures.addCallback(loginFuture, new FutureCallback<LoginData>() {
            @Override
            public void onSuccess(final LoginData result) {
                if (getCallingActivity() == null) {
                    final Intent savePin = PinSaveActivity.createIntent(MnemonicActivity.this, mService.getMnemonic());
                    startActivityForResult(savePin, PINSAVE);
                } else {
                    setResult(RESULT_OK);
                    finishOnUiThread();
                }
            }

            @Override
            public void onFailure(final Throwable t) {
                final boolean accountDoesntExist = t instanceof ClassCastException;
                final String message = accountDoesntExist ? "Account doesn't exist" : "Login failed";
                t.printStackTrace();
                MnemonicActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        MnemonicActivity.this.toast(message);
                        enableLogin();
                    }
                });
            }
        }, mService.getExecutor());
    }

    private ListenableFuture<String> askForPassphrase() {
        final SettableFuture<String> fn = SettableFuture.create();
        runOnUiThread(new Runnable() {
            public void run() {
                final View v = getLayoutInflater().inflate(R.layout.dialog_passphrase, null, false);
                final EditText passphraseValue = UI.find(v, R.id.passphraseValue);
                passphraseValue.requestFocus();
                final MaterialDialog dialog = UI.popup(MnemonicActivity.this, "Encryption passphrase")
                        .customView(v, true)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(final MaterialDialog dlg, final DialogAction which) {
                                fn.set(UI.getText(passphraseValue));
                            }
                        })
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(final MaterialDialog dlg, final DialogAction which) {
                                enableLogin();
                            }
                        }).build();
                UI.showDialog(dialog);
            }
        });
        return fn;
    }

    @Override
    public void onClick(final View v) {
        if (v == mOkButton)
            doLogin();
        else if (v == mScanButton)
            onScanClicked();
    }

    private void onScanClicked() {
        final String[] perms = { "android.permission.CAMERA" };
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1 &&
            checkSelfPermission(perms[0]) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(perms, CAMERA_PERMISSION);
        else {
            final Intent scanner = new Intent(MnemonicActivity.this, ScanActivity.class);
            startActivityForResult(scanner, QRSCANNER);
        }
    }

    private void loginOnUiThread() {
        if (mService.onConnected == null || getMnemonic().equals(mService.getMnemonic()))
            return;

        CB.after(mService.onConnected, new CB.Op<Void>() {
            @Override
            public void onSuccess(final Void result) {
                runOnUiThread(new Runnable() { public void run() { doLogin(); } });
            }
        });
    }

    private static byte[] getNFCPayload(final Intent intent) {
        final Parcelable[] extra = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        return ((NdefMessage) extra[0]).getRecords()[0].getPayload();
    }

     private void NFCIntentMnemonicLogin() {
        final Intent intent = getIntent();

        if (intent == null || !NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction()))
            return;

        mMnemonicText.setTextColor(Color.WHITE);

        if (intent.getType().equals("x-gait/mnc")) {
            // Unencrypted NFC
            mMnemonicText.setText(CryptoHelper.mnemonic_from_bytes(getNFCPayload(intent)));
            loginOnUiThread();

        } else if (intent.getType().equals("x-ga/en"))
            // Encrypted NFC
            CB.after(askForPassphrase(), new CB.Op<String>() {
                @Override
                public void onSuccess(final String passphrase) {
                    mMnemonicText.setText(CryptoHelper.decrypt_mnemonic(getNFCPayload(intent),
                                                                        passphrase));
                    loginOnUiThread();
                }
            });
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case PINSAVE:
                onLoginSuccess();
                break;
            case QRSCANNER:
                if (data != null && data.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT) != null) {
                    mMnemonicText.setText(data.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT));
                    doLogin();
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // FIXME: Show connectivity status to user
        // Inflate the menu; this adds items to the action bar if it is present.
        // getMenuInflater().inflate(R.menu.mnemonic, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        return item.getItemId() == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, final String[] permissions, final int[] grantResults) {
        if (requestCode != CAMERA_PERMISSION)
            return;

        if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            shortToast(R.string.err_qrscan_requires_camera_permissions);
            return;
        }

        final Intent scanner = new Intent(MnemonicActivity.this, ScanActivity.class);
        startActivityForResult(scanner, QRSCANNER);
    }
}
