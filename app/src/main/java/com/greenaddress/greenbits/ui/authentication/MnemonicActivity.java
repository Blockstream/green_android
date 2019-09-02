package com.greenaddress.greenbits.ui.authentication;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.StrikethroughSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.blockstream.libgreenaddress.GDK;
import com.blockstream.libwally.Wally;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.greenaddress.greenapi.ConnectionManager;
import com.greenaddress.greenapi.CryptoHelper;
import com.greenaddress.greenbits.ui.CB;
import com.greenaddress.greenbits.ui.LoginActivity;
import com.greenaddress.greenapi.MnemonicHelper;
import com.greenaddress.greenbits.ui.onboarding.PinSaveActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.ScanForResultActivity;
import com.greenaddress.greenbits.ui.TabbedMainActivity;
import com.greenaddress.greenbits.ui.UI;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;

public class MnemonicActivity extends LoginActivity implements View.OnClickListener,
    View.OnKeyListener, TextView.OnEditorActionListener {

    private static final String TAG = MnemonicActivity.class.getSimpleName();
    public static final String TEMPORARY_MODE = "TEMPORANY_MODE";

    private static final int PINSAVE = 1337;
    private static final int QRSCANNER = 1338;
    private static final int CAMERA_PERMISSION = 150;
    private static final int MNEMONIC_LENGTH = 24;
    private static final int ENCRYPTED_MNEMONIC_LENGTH = 27;

    private final Object mWordList = Wally.bip39_get_wordlist("en");

    private Button mOkButton;
    private Switch mEncryptedSwitch;
    private final MultiAutoCompleteTextView mWordEditTexts[] = new MultiAutoCompleteTextView[ENCRYPTED_MNEMONIC_LENGTH];
    private ArrayAdapter<String> mWordsAdapter;

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
            return t;
        }
    };

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {
        Log.d(TAG, getIntent().getType() + ' ' + getIntent());
        setTitleBackTransparent();
        setTitleWithNetwork(R.string.id_restore);

        UI.preventScreenshots(this);
        mOkButton = UI.mapClick(this, R.id.mnemonicOkButton, this);
        mEncryptedSwitch = UI.mapClick(this, R.id.mnemonicEncrypted, this);

        mWordsAdapter =
            new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, MnemonicHelper.mWordsArray);

        setUpTable(R.id.mnemonic24, 1);
        setUpTable(R.id.mnemonic3, 25);

        mOkButton.setEnabled(false);
        NFCIntentMnemonicLogin();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        UI.unmapClick(mOkButton);
        UI.unmapClick(mEncryptedSwitch);
    }

    private void setUpTable(final int id, final int startWordNum) {
        int wordNum = startWordNum;
        final TableLayout table = UI.find(this, id);

        for (int y = 0; y < table.getChildCount(); ++y) {
            final TableRow row = (TableRow) table.getChildAt(y);

            for (int x = 0; x < row.getChildCount() / 2; ++x) {
                ((TextView) row.getChildAt(x * 2)).setText(String.valueOf(wordNum));

                MultiAutoCompleteTextView me = (MultiAutoCompleteTextView) row.getChildAt(x * 2 + 1);
                me.setAdapter(mWordsAdapter);
                me.setThreshold(3);
                me.setTokenizer(mTokenizer);
                me.setOnEditorActionListener(this);
                me.setOnKeyListener(this);
                me.addTextChangedListener(new UI.TextWatcher() {
                    @Override
                    public void afterTextChanged(final Editable s) {
                        super.afterTextChanged(s);
                        final String original = s.toString();
                        final String trimmed = original.trim();
                        if (!trimmed.isEmpty() && !trimmed.equals(original)) {
                            me.setText(trimmed);
                            return;
                        }
                        final boolean isInvalid = markInvalidWord(s);
                        if (!isInvalid && (s.length() > 3)) {
                            if (!enableLogin())
                                nextFocus();
                        }
                        enableLogin();
                    }
                });
                me.setOnFocusChangeListener((View v, boolean hasFocus) -> {
                    if (!hasFocus && v instanceof EditText) {
                        final Editable e = ((EditText)v).getEditableText();
                        final String word = e.toString();
                        if (!MnemonicHelper.mWords.contains(word)) {
                            e.setSpan(new StrikethroughSpan(), 0, word.length(), 0);
                        }
                    }
                });
                registerForContextMenu(me);

                mWordEditTexts[wordNum - 1] = me;
                ++wordNum;
            }
        }
    }

    protected int getMainViewId() { return R.layout.activity_mnemonic; }

    private String getMnemonic() {
        StringBuilder sb = new StringBuilder();
        for (MultiAutoCompleteTextView me : mWordEditTexts)
            sb.append(me.getText()).append(" ");
        return sb.toString().trim();
    }

    private void setMnemonic(String mnemonic) {
        mnemonic = mnemonic.trim().replaceAll("(\\r|\\n)","").toLowerCase();
        final int errId = checkValid(mnemonic);
        if (errId != 0) {
            UI.toast(this, errId, Toast.LENGTH_LONG);
            return;
        }

        final String words[] = mnemonic.split(" ");
        mEncryptedSwitch.setChecked(words.length == ENCRYPTED_MNEMONIC_LENGTH);
        onClick(mEncryptedSwitch);
        for (int i  = 0; i < words.length; ++i) {
            mWordEditTexts[i].setText(words[i]);
        }
    }



    private int checkValid(final String mnemonic) {
        final String words[] = mnemonic.split(" ");

        //validate hex_seed
        if (isHexSeed(words[0])) {
            mOkButton.setEnabled(true);
            return 0;
        }

        if (words.length != MNEMONIC_LENGTH && words.length != ENCRYPTED_MNEMONIC_LENGTH)
            return R.string.id_invalid_mnemonic_must_be_24_or;
        try {
            Wally.bip39_mnemonic_validate(mWordList, mnemonic);
        } catch (final IllegalArgumentException e) {
            return R.string.id_invalid_mnemonic;
        }
        return 0;
    }

    private boolean enableLogin() {
        stopLoading();
        final boolean valid = checkValid(getMnemonic()) == 0;
        if (valid != mOkButton.isEnabled()) {
            mOkButton.setVisibility(View.VISIBLE);
            mOkButton.setEnabled(valid);
        }
        return valid;
    }

    private void doLogin() {
        if (isLoading())
            return;

        final ConnectionManager cm = mService.getConnectionManager();
        if (cm.isPostLogin()) {
            toast(R.string.id_you_must_first_log_out_before);
            return;
        }

        final String mnemonic = getMnemonic();
        final int errId = checkValid(mnemonic);
        if (errId != 0) {
            UI.toast(this, errId, Toast.LENGTH_LONG);
            return;
        }


        if (isHexSeed(mnemonic)) {
            mService.getExecutor().execute(() -> {
                mService.resetSession();
                cm.loginWithMnemonic(mnemonic, "");
            });
        } else if (!mEncryptedSwitch.isChecked()) {
            mService.getExecutor().execute(() -> {
                mService.resetSession();
                cm.loginWithMnemonic(mnemonic, "");
            });
        } else {
            CB.after(askForPassphrase(), new CB.Toast<String>(this, mOkButton) {
                @Override
                public void onSuccess(final String mnemonicPassword) {
                    mService.resetSession();
                    cm.loginWithMnemonic(mnemonic, mnemonicPassword);
                }
            });
        }

        startLoading();
        mOkButton.setEnabled(false);
    }

    private ListenableFuture<String> askForPassphrase() {
        final SettableFuture<String> fn = SettableFuture.create();
        runOnUiThread(() -> {
            final View v = UI.inflateDialog(MnemonicActivity.this, R.layout.dialog_passphrase);
            final EditText passEdit = UI.find(v, R.id.passphraseValue);
            passEdit.requestFocus();
            final MaterialDialog d = UI.popup(MnemonicActivity.this, "Encryption passphrase")
                                     .customView(v, true)
                                     .backgroundColor(getResources().getColor(R.color.buttonJungleGreen))
                                     .onPositive((dlg, w) -> fn.set(UI.getText(passEdit)))
                                     .onNegative((dlg, w) -> enableLogin())
                                     .build();
            UI.mapEnterToPositive(d, R.id.passphraseValue);
            UI.showDialog(d);
        });
        return fn;
    }

    @Override
    public void onClick(final View v) {
        if (v == mOkButton)
            doLogin();
        else if (v == mEncryptedSwitch) {
            final boolean encrypted = mEncryptedSwitch.isChecked();
            UI.showIf(encrypted, UI.find(this, R.id.mnemonic3));
            if (!encrypted) {
                for (int i = MNEMONIC_LENGTH; i < ENCRYPTED_MNEMONIC_LENGTH; ++i)
                    mWordEditTexts[i].setText("");
            }
        }
    }

    private void onScanClicked() {
        final String[] perms = { "android.permission.CAMERA" };
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1 &&
            checkSelfPermission(perms[0]) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(perms, CAMERA_PERMISSION);
        else {
            final Intent scanner = new Intent(MnemonicActivity.this, ScanForResultActivity.class);
            startActivityForResult(scanner, QRSCANNER);
        }
    }

    private static byte[] getNFCPayload(final Intent intent) {
        final Parcelable[] extra = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        return ((NdefMessage) extra[0]).getRecords()[0].getPayload();
    }

    private void NFCIntentMnemonicLogin() {
        final Intent intent = getIntent();

        if (intent == null || !NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction()))
            return;

        if ("x-gait/mnc".equals(intent.getType())) {
            // Unencrypted NFC
            setMnemonic(CryptoHelper.mnemonic_from_bytes(getNFCPayload(intent)));
            runOnUiThread(this::doLogin);
        } else if ("x-ga/en".equals(intent.getType()))
            // Encrypted NFC
            CB.after(askForPassphrase(), new CB.Op<String>() {
                @Override
                public void onSuccess(final String passphrase) {
                    setMnemonic(CryptoHelper.decrypt_mnemonic(getNFCPayload(intent), passphrase));
                    runOnUiThread(() -> doLogin());
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
            if (data != null && data.getStringExtra(ScanForResultActivity.INTENT_EXTRA_RESULT) != null) {
                setMnemonic(data.getStringExtra(ScanForResultActivity.INTENT_EXTRA_RESULT));
                doLogin();
            }
            break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.mnemonic, menu);
        menu.findItem(R.id.action_scan).setIcon(R.drawable.ic_qr);
        return true;
    }


    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View v,
                                    final ContextMenu.ContextMenuInfo menuInfo) {
        // Handle custom paste
        menu.add(0, v.getId(), 0, getString(R.string.id_paste));
    }

    @Override
    public boolean onContextItemSelected(final MenuItem menuItem) {
        final ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard.hasPrimaryClip() && clipboard.getPrimaryClipDescription().hasMimeType(MIMETYPE_TEXT_PLAIN)) {
            final ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
            setMnemonic(item.getText().toString());
        }
        return super.onContextItemSelected(menuItem);
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu);
        final boolean haveCamera = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
        setMenuItemVisible(menu, R.id.action_add, haveCamera);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            onBackPressed();
            return true;
        case R.id.action_scan:
            onScanClicked();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, final String[] permissions, final int[] granted) {
        if (requestCode == CAMERA_PERMISSION &&
            isPermissionGranted(granted, R.string.id_please_enable_camera))
            startActivityForResult(new Intent(this, ScanForResultActivity.class), QRSCANNER);
    }

    @Override
    protected void onLoginSuccess() {
        super.onLoginSuccess();
        if (getCallingActivity() == null) {
            if (getIntent().getBooleanExtra(TEMPORARY_MODE, false)) {
                final Intent intent = new Intent(this, TabbedMainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            } else {
                final Intent savePin = PinSaveActivity.createIntent(MnemonicActivity.this, mService.getMnemonic());
                startActivityForResult(savePin, PINSAVE);
            }
        } else {
            setResult(RESULT_OK);
            finishOnUiThread();
        }
    }

    @Override
    protected void onLoginFailure() {
        super.onLoginFailure();
        final Exception lastLoginException = mService.getConnectionManager().getLastLoginException();
        final int code = getCode(lastLoginException);
        mService.getConnectionManager().clearPreviousLoginError();
        if (code == GDK.GA_RECONNECT) {
            UI.toast(this, R.string.id_you_are_not_connected_to_the, Toast.LENGTH_LONG);
        } else {
            UI.toast(this, R.string.id_login_failed, Toast.LENGTH_LONG);
        }

        enableLogin();
    }

    @Override
    public boolean onEditorAction(final TextView textView, final int actionId, final KeyEvent keyEvent) {
        if (keyEvent != null) {
            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER ||
                keyEvent.getKeyCode() == KeyEvent.KEYCODE_SPACE) {
                nextFocus();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onKey(final View view, final int keyCode, final KeyEvent keyEvent) {
        if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_SPACE) {
            nextFocus();
            return true;
        }
        return false;
    }

    private boolean markInvalidWord(final Editable e) {
        for (final StrikethroughSpan s : e.getSpans(0, e.length(), StrikethroughSpan.class))
            e.removeSpan(s);

        final String word = e.toString();
        if (isHexSeed(word))
            return true;
        final int end = word.length();
        if (!MnemonicHelper.isPrefix(word))
            e.setSpan(new StrikethroughSpan(), 0, end, 0);
        return !MnemonicHelper.mWords.contains(word);
    }

    private void nextFocus() {
        final View view = getCurrentFocus();
        if (!(view instanceof TextView))
            return;
        final View next = view.focusSearch(View.FOCUS_FORWARD);
        if (next != null)
            next.requestFocus();
    }
}
