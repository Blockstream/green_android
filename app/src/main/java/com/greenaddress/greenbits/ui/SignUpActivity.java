package com.greenaddress.greenbits.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.BitmapDrawable;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.dd.CircularProgressButton;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.greenaddress.greenapi.CryptoHelper;
import com.greenaddress.greenapi.LoginData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;


public class SignUpActivity extends LoginActivity implements View.OnClickListener {
    private static final int PINSAVE = 1337;
    private static final int VERIFY_COUNT = 4;

    private boolean mWriteMode;
    private Dialog mMnemonicDialog;
    private Dialog mNfcDialog;
    private Dialog mVerifyDialog;
    private NfcAdapter mNfcAdapter;
    private PendingIntent mNfcPendingIntent;
    private View mNfcView;
    private ImageView mNfcSignupIcon;

    private TextView mMnemonicText;
    private CheckBox mAcceptCheckBox;
    private CircularProgressButton mContinueButton;
    private TextView mQrCodeIcon;
    private ImageView mQrCodeBitmap;

    private ArrayList<Integer> mWordChoices;
    private boolean[] mChoiceIsValid;

    private ListenableFuture<LoginData> mOnSignUp;
    private final Runnable mNfcDialogCB = new Runnable() { public void run() { mWriteMode = false; } };
    private final Runnable mVerifyDialogCB = new Runnable() { public void run() { onVerifyDismissed(); } };

    @Override
    protected int getMainViewId() { return R.layout.activity_sign_up; }

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        mNfcPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, SignUpActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        mNfcView = getLayoutInflater().inflate(R.layout.dialog_nfc_write, null, false);

        mMnemonicText = UI.find(this, R.id.signupMnemonicText);
        mQrCodeIcon = UI.find(this, R.id.signupQrCodeIcon);
        mAcceptCheckBox = UI.find(this, R.id.signupAcceptCheckBox);
        mContinueButton = UI.find(this, R.id.signupContinueButton);
        mNfcSignupIcon = UI.find(this, R.id.signupNfcIcon);

        mMnemonicText.setText(mService.getSignUpMnemonic());

        if (mOnSignUp != null) {
            UI.disable(mAcceptCheckBox);
            mAcceptCheckBox.setChecked(true);
            UI.enable(mContinueButton);
        }

        final TextView termsText = UI.find(this, R.id.textTosLink);
        termsText.setMovementMethod(LinkMovementMethod.getInstance());

        mQrCodeIcon.setOnClickListener(this);
        mContinueButton.setOnClickListener(this);

        mNfcSignupIcon.setOnClickListener(this);

        mWordChoices = new ArrayList<>(24);
        for (int i = 0; i < 24; ++i)
            mWordChoices.add(i);

        mChoiceIsValid = new boolean[VERIFY_COUNT];
    }

    @Override
    public void onResumeWithService() {
        if (mNfcAdapter != null) {
            final IntentFilter filter = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
            final IntentFilter[] filters = new IntentFilter[]{filter};
            mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, filters, null);
        }
        UI.showIf(mNfcAdapter != null && mNfcAdapter.isEnabled(), mNfcSignupIcon);
    }

    @Override
    public void onPauseWithService() {
        if (mNfcAdapter != null)
            mNfcAdapter.disableForegroundDispatch(this);
        if (mContinueButton != null) {
            mContinueButton.setIndeterminateProgressMode(false);
            mContinueButton.setProgress(0);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        UI.unmapClick(mQrCodeIcon);
        UI.unmapClick(mContinueButton);
        UI.unmapClick(mNfcSignupIcon);

        mMnemonicDialog = UI.dismiss(this, mMnemonicDialog);
        mNfcDialog = UI.dismiss(this, mNfcDialog);
        mNfcView = null;
        if (mChoiceIsValid != null)
            mChoiceIsValid[0] = false;
        mVerifyDialog = UI.dismiss(this, mVerifyDialog);
    }

    @Override
    public void onClick(final View v) {
        if (v == mQrCodeIcon)
            onQrCodeButtonClicked();
        else if (v == mContinueButton)
            onContinueButtonClicked();
        else if (v == mNfcSignupIcon)
            onNfcSignupButtonClicked();
    }

    private void onQrCodeButtonClicked() {
        if (mMnemonicDialog == null) {
            final View v = getLayoutInflater().inflate(R.layout.dialog_qrcode, null, false);
            mQrCodeBitmap = UI.find(v, R.id.qrInDialogImageView);
            mQrCodeBitmap.setLayoutParams(UI.getScreenLayout(SignUpActivity.this, 0.8));
            mMnemonicDialog = new Dialog(SignUpActivity.this);
            mMnemonicDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            mMnemonicDialog.setContentView(v);
        }
        mMnemonicDialog.show();
        final BitmapDrawable bd = new BitmapDrawable(getResources(), mService.getSignUpQRCode());
        bd.setFilterBitmap(false);
        mQrCodeBitmap.setImageDrawable(bd);
    }

    private void onContinueButtonClicked() {
        int errorId = 0;
        if (!mService.isConnected())
            errorId = R.string.notConnected;
        else if (!mAcceptCheckBox.isChecked())
            errorId = R.string.securePassphraseMsg;
        else if (mOnSignUp != null)
            errorId = R.string.signupInProgress;

        if (errorId != 0) {
            toast(errorId);
            return;
        }

        mContinueButton.setIndeterminateProgressMode(true);
        mContinueButton.setProgress(50);
        UI.hide(mMnemonicText, mQrCodeIcon);

        // Create a random shuffle of word orders; the user will be asked
        // to verify the first VERIFY_COUNT words.
        Collections.shuffle(mWordChoices);

        for (int i = 0; i < mChoiceIsValid.length; ++i)
            mChoiceIsValid[i] = false;

        // Show the verification dialog
        final View v = getLayoutInflater().inflate(R.layout.dialog_verify_words, null, false);
        mVerifyDialog = new MaterialDialog.Builder(SignUpActivity.this)
                .title(R.string.enter_matching_words)
                .customView(v, true)
                .titleColorRes(R.color.white)
                .contentColorRes(android.R.color.white)
                .theme(Theme.DARK)
                .build();
        UI.setDialogCloseHandler(mVerifyDialog, mVerifyDialogCB, false);
        final String[] words = UI.getText(mMnemonicText).split(" ");
        setupWord(v, R.id.verify_label_1, R.id.verify_word_1, words, 0);
        setupWord(v, R.id.verify_label_2, R.id.verify_word_2, words, 1);
        setupWord(v, R.id.verify_label_3, R.id.verify_word_3, words, 2);
        setupWord(v, R.id.verify_label_4, R.id.verify_word_4, words, 3);
        mVerifyDialog.show();
    }

    private void onMnemonicVerified() {
        mOnSignUp = mService.signup(UI.getText(mMnemonicText));
        Futures.addCallback(mOnSignUp, new FutureCallback<LoginData>() {
            @Override
            public void onSuccess(final LoginData result) {
                onSignUpCompleted();
            }

            @Override
            public void onFailure(final Throwable t) {
                setComplete(false);
                mOnSignUp = null;
                t.printStackTrace();
                toast(t.getMessage());
            }
        }, mService.getExecutor());
    }

    private void onNfcSignupButtonClicked() {
        if (mNfcDialog == null) {
            mNfcDialog = new MaterialDialog.Builder(SignUpActivity.this)
                .title(R.string.nfcDialogMessage)
                .customView(mNfcView, true)
                .titleColorRes(R.color.white)
                .contentColorRes(android.R.color.white)
                .theme(Theme.DARK).build();
            UI.setDialogCloseHandler(mNfcDialog, mNfcDialogCB, true /* cancelOnly */);
        }
        mWriteMode = true;
        mNfcDialog.show();
    }

    private void setComplete(final boolean isComplete) {
        runOnUiThread(new Runnable() {
            public void run() {
                mContinueButton.setProgress(isComplete ? 100 : 0);
            }
        });
    }

    private void onSignUpCompleted() {
        setComplete(true);
        mService.resetSignUp();
        mOnSignUp = null;
        final Intent savePin = PinSaveActivity.createIntent(SignUpActivity.this, mService.getMnemonic());
        startActivityForResult(savePin, PINSAVE);
    }

    private void incrementTagsWritten() {
        final TextView tagsWrittenText = UI.find(mNfcView, R.id.nfcTagsWrittenText);
        final Integer written = Integer.parseInt(UI.getText(tagsWrittenText));
        tagsWrittenText.setText(String.valueOf(written + 1));
    }

    @Override
    @SuppressLint("NewApi") // mNfcSignupIcon is hidden for API < 16
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);

        if (!mWriteMode || !NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction()))
            return;

        final Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        final byte[] seed = CryptoHelper.mnemonic_to_bytes(UI.getText(mMnemonicText));

        final NdefRecord[] record = new NdefRecord[1];
        record[0] = NdefRecord.createMime("x-gait/mnc", seed);

        final NdefMessage message = new NdefMessage(record);
        final int size = message.toByteArray().length;
        try {
            final Ndef ndef = Ndef.get(detectedTag);
            if (ndef != null) {
                ndef.connect();
                if (!ndef.isWritable())
                    shortToast(R.string.err_sign_up_nfc_not_writable);
                if (ndef.getMaxSize() < size)
                    shortToast(R.string.err_sign_up_nfc_too_small);
                ndef.writeNdefMessage(message);
                incrementTagsWritten();
            } else {
                final NdefFormatable format = NdefFormatable.get(detectedTag);
                if (format != null)
                    try {
                        format.connect();
                        format.format(message);
                        incrementTagsWritten();
                    } catch (final IOException e) {
                    }
            }
        } catch (final Exception e) {
        }
    }

    @Override
    public void onBackPressed() {

        if (mOnSignUp != null) {
            mService.resetSignUp();
            mOnSignUp = null;
            mService.disconnect(true);
        }
        super.onBackPressed();
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
        return item.getItemId() == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case PINSAVE:
                onLoginSuccess();
                break;
        }
    }

    void setupWord(final View v, final int labelId, final int spinnerId,
                   final String[] words, final int index) {
        final int wordIndex = mWordChoices.get(index);
        final String validWord = words[wordIndex];

        final TextView label = UI.find(v, labelId);
        final AutoCompleteTextView text = UI.find(v, spinnerId);

        label.setText(getString(R.string.hash_number, wordIndex + 1));
        final ArrayAdapter<String> adapter;
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, MnemonicHelper.mWordsArray);
        text.setAdapter(adapter);
        text.setThreshold(1);
        text.addTextChangedListener(new UI.TextWatcher() {
            @Override
            public void onTextChanged(final CharSequence t, final int start,
                                      final int before, final int count) {
                final AutoCompleteTextView tv = UI.find(v, spinnerId);
                onWordChanged(label, tv, index, validWord, true);
            }
        });
        text.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
                final AutoCompleteTextView tv = UI.find(v, spinnerId);
                onWordChanged(label, tv, index, validWord, false);
            }
        });
    }

    private void onWordChanged(final TextView label, final AutoCompleteTextView text,
                               final int index, final String validWord, final boolean isTextChange) {
        if (isTextChange && text.isPerformingCompletion())
            return; // Let the call from onItemClick handle it
        final boolean isValid = UI.getText(text).equals(validWord);
        mChoiceIsValid[index] = isValid;
        if (isValid) {
            UI.hide(label, text);
            if (areAllChoicesValid())
                UI.dismiss(this, mVerifyDialog); // Dismiss callback will continue
        }
    }

    private boolean areAllChoicesValid() {
        if (mChoiceIsValid == null)
            return false;
        for (final boolean isValid : mChoiceIsValid)
            if (!isValid)
                return false;
        return true;
    }

    private void onVerifyDismissed() {
        if (mVerifyDialog != null) {
            UI.show(mMnemonicText, mQrCodeIcon);
            mContinueButton.setIndeterminateProgressMode(false);
            mContinueButton.setProgress(0);
            mVerifyDialog = null;
            if (areAllChoicesValid())
                onMnemonicVerified();
        }
    }
}
