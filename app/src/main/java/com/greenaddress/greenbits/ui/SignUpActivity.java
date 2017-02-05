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
import android.os.Build;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
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

public class SignUpActivity extends LoginActivity {
    private static final String TAG = SignUpActivity.class.getSimpleName();
    private static final int PINSAVE = 1337;

    private boolean mWriteMode;
    private Dialog mMnemonicDialog;
    private Dialog mNfcDialog;
    private NfcAdapter mNfcAdapter;
    private PendingIntent mNfcPendingIntent;
    private TextView mNfcTagsWrittenText;
    private ImageView mSignupNfcIcon;

    private TextView mMnemonicText;
    private CheckBox mAcceptCheckBox;
    private CircularProgressButton mContinueButton;

    private ListenableFuture<LoginData> mOnSignUp;
    private final Runnable mDialogCB = new Runnable() { public void run() { mWriteMode = false; } };

    @Override
    protected int getMainViewId() { return R.layout.activity_sign_up; }

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {

        final View nfcView = getLayoutInflater().inflate(R.layout.dialog_nfc_write, null, false);
        mNfcTagsWrittenText = UI.find(nfcView, R.id.nfcTagsWrittenText);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        mNfcPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, SignUpActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        mMnemonicText = UI.find(this, R.id.signupMnemonicText);
        mMnemonicText.setText(mService.getSignUpMnemonic());

        mAcceptCheckBox = UI.find(this, R.id.signupAcceptCheckBox);
        mContinueButton = UI.find(this, R.id.signupContinueButton);

        if (mOnSignUp != null) {
            mAcceptCheckBox.setEnabled(false);
            mAcceptCheckBox.setChecked(true);
            UI.enable(mContinueButton);
        }

        final TextView termsText = UI.find(this, R.id.textTosLink);
        termsText.setMovementMethod(LinkMovementMethod.getInstance());

        final View qrView = getLayoutInflater().inflate(R.layout.dialog_qrcode, null, false);

        final ImageView qrCodeMnemonic = UI.find(qrView, R.id.qrInDialogImageView);
        final TextView qrCodeIcon = UI.find(this, R.id.signupQrCodeIcon);

        qrCodeIcon.setOnClickListener(new View.OnClickListener() {
            public void onClick(final View v) {
                qrCodeIcon.clearAnimation();
                if (mMnemonicDialog == null) {
                    qrCodeMnemonic.setLayoutParams(UI.getScreenLayout(SignUpActivity.this, 0.8));

                    mMnemonicDialog = new Dialog(SignUpActivity.this);
                    mMnemonicDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
                    mMnemonicDialog.setContentView(qrView);
                }
                mMnemonicDialog.show();
                final BitmapDrawable bd = new BitmapDrawable(getResources(), mService.getSignUpQRCode());
                bd.setFilterBitmap(false);
                qrCodeMnemonic.setImageDrawable(bd);
            }
        });

        mContinueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
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

                mOnSignUp = mService.signup(UI.getText(mMnemonicText));
                Futures.addCallback(mOnSignUp, new FutureCallback<LoginData>() {
                    @Override
                    public void onSuccess(final LoginData result) {
                        setComplete(true);
                        mService.resetSignUp();
                        mOnSignUp = null;
                        final Intent savePin = PinSaveActivity.createIntent(SignUpActivity.this, mService.getMnemonics());
                        startActivityForResult(savePin, PINSAVE);
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
        });

        mSignupNfcIcon = UI.find(this, R.id.signupNfcIcon);
        if (Build.VERSION.SDK_INT < 16) {
            UI.hide(mSignupNfcIcon);
            return;
        }

        mNfcDialog = new MaterialDialog.Builder(SignUpActivity.this)
                .title(R.string.nfcDialogMessage)
                .customView(nfcView, true)
                .titleColorRes(R.color.white)
                .contentColorRes(android.R.color.white)
                .theme(Theme.DARK).build();

        mSignupNfcIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                mWriteMode = true;
                mNfcDialog.show();
            }
        });
        UI.setDialogCloseHandler(mNfcDialog, mDialogCB, true /* cancelOnly */);
    }

    private void setComplete(final boolean isComplete) {
        runOnUiThread(new Runnable() {
            public void run() {
                mContinueButton.setProgress(isComplete ? 100 : 0);
            }
        });
    }

    private void incrementTagsWritten() {
        final Integer written = Integer.parseInt(UI.getText(mNfcTagsWrittenText));
        mNfcTagsWrittenText.setText(String.valueOf(written + 1));
    }

    @Override
    public void onResumeWithService() {
        if (mNfcAdapter != null) {
            final IntentFilter filter = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
            final IntentFilter[] filters = new IntentFilter[]{filter};
            mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, filters, null);
        }
        UI.showIf(mNfcAdapter != null && mNfcAdapter.isEnabled(), mSignupNfcIcon);
    }

    @Override
    public void onPauseWithService() {
        if (mNfcAdapter != null)
            mNfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    @SuppressLint("NewApi") // mSignupNfcIcon is hidden for API < 16
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
    public void onDestroy() {
        super.onDestroy();
        if (mMnemonicDialog != null)
            mMnemonicDialog.dismiss();
        if (mNfcDialog != null)
            mNfcDialog.dismiss();
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
}
