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
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.dd.CircularProgressButton;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.greenaddress.greenapi.CryptoHelper;
import com.greenaddress.greenapi.LoginData;

import java.io.IOException;

public class SignUpActivity extends LoginActivity {
    private static final String TAG = SignUpActivity.class.getSimpleName();
    private static final int PINSAVE = 1337;

    private boolean mWriteMode = false;
    private Dialog mnemonicDialog;
    private Dialog nfcDialog;
    private NfcAdapter mNfcAdapter;
    private PendingIntent mNfcPendingIntent;
    private TextView nfcTagsWritten;
    private ImageView signupNfcIcon;
    private TextView mnemonicText;
    private ListenableFuture<LoginData> onSignUp;
    private final Runnable mDialogCB = new Runnable() { public void run() { mWriteMode = false; } };

    @Override
    protected int getMainViewId() { return R.layout.activity_sign_up; }

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {

        final CircularProgressButton signupContinueButton = UI.find(this, R.id.signupContinueButton);
        final TextView tos = UI.find(this, R.id.textTosLink);
        final CheckBox checkBox = UI.find(this, R.id.signupAcceptCheckBox);
        final View nfcView = getLayoutInflater().inflate(R.layout.dialog_nfc_write, null, false);
        nfcTagsWritten = UI.find(nfcView, R.id.nfcTagsWrittenText);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        mNfcPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, SignUpActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        if (onSignUp != null) {
            checkBox.setEnabled(false);
            checkBox.setChecked(true);
        }

        tos.setMovementMethod(LinkMovementMethod.getInstance());

        mnemonicText = UI.find(this, R.id.signupMnemonicText);

        final View qrView = getLayoutInflater().inflate(R.layout.dialog_qrcode, null, false);

        final TextView qrCodeIcon = UI.find(this, R.id.signupQrCodeIcon);
        final ImageView qrcodeMnemonic = UI.find(qrView, R.id.qrInDialogImageView);
        mnemonicText.setText(mService.getSignUpMnemonic());

        qrCodeIcon.setOnClickListener(new View.OnClickListener() {
            public void onClick(final View v) {
                qrCodeIcon.clearAnimation();
                if (mnemonicDialog == null) {
                    qrcodeMnemonic.setLayoutParams(UI.getScreenLayout(SignUpActivity.this, 0.8));

                    mnemonicDialog = new Dialog(SignUpActivity.this);
                    mnemonicDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
                    mnemonicDialog.setContentView(qrView);
                }
                mnemonicDialog.show();
                final BitmapDrawable bd = new BitmapDrawable(getResources(), mService.getSignUpQRCode());
                bd.setFilterBitmap(false);
                qrcodeMnemonic.setImageDrawable(bd);
            }
        });

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton compoundButton, final boolean isChecked) {
                if (onSignUp == null)
                    if (mService.onConnected != null) {
                        signupContinueButton.setEnabled(true);
                        checkBox.setEnabled(false);
                        onSignUp = Futures.transform(mService.onConnected, new AsyncFunction<Void, LoginData>() {
                            @Override
                            public ListenableFuture<LoginData> apply(final Void input) throws Exception {
                                return mService.signup(UI.getText(mnemonicText));
                            }
                        }, mService.getExecutor());
                    } else if (isChecked) {
                        SignUpActivity.this.toast(R.string.notConnected);
                        checkBox.setChecked(false);
                    }
            }
        });

        signupContinueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (onSignUp != null) {

                    signupContinueButton.setIndeterminateProgressMode(true);
                    signupContinueButton.setProgress(50);
                    Futures.addCallback(onSignUp, new FutureCallback<LoginData>() {

                        @Override
                        public void onSuccess(final LoginData result) {
                            SignUpActivity.this.runOnUiThread(new Runnable() {
                                public void run() {
                                    signupContinueButton.setProgress(100);
                                }
                            });

                            mService.resetSignUp();
                            onSignUp = null;
                            final Intent savePin = PinSaveActivity.createIntent(SignUpActivity.this, mService.getMnemonics());
                            startActivityForResult(savePin, PINSAVE);
                        }

                        @Override
                        public void onFailure(final Throwable t) {
                            t.printStackTrace();
                            SignUpActivity.this.runOnUiThread(new Runnable() {
                                public void run() {
                                    signupContinueButton.setProgress(0);
                                }
                            });
                        }
                    }, mService.getExecutor());
                } else
                    if (!checkBox.isChecked())
                        SignUpActivity.this.toast(R.string.securePassphraseMsg);
                    else
                        SignUpActivity.this.toast(R.string.signupInProgress);
            }
        });
        signupNfcIcon = UI.find(this, R.id.signupNfcIcon);

        nfcDialog = new MaterialDialog.Builder(SignUpActivity.this)
                .title(R.string.nfcDialogMessage)
                .customView(nfcView, true)
                .titleColorRes(R.color.white)
                .contentColorRes(android.R.color.white)
                .theme(Theme.DARK).build();

        if (Build.VERSION.SDK_INT < 16)
            UI.hide(signupNfcIcon);
        else
            signupNfcIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    mWriteMode = true;
                    nfcDialog.show();
                }
            });

        UI.setDialogCloseHandler(nfcDialog, mDialogCB, true /* cancelOnly */);
    }

    @Override
    public void onResumeWithService() {
        if (mNfcAdapter != null) {
            final IntentFilter filter = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
            final IntentFilter[] filters = new IntentFilter[]{filter};
            mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, filters, null);
        }
        UI.showIf(mNfcAdapter != null && mNfcAdapter.isEnabled(), signupNfcIcon);
    }

    @Override
    public void onPauseWithService() {
        if (mNfcAdapter != null)
            mNfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    @SuppressLint("NewApi") // signupNfcIcon is hidden for API < 16
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        if (mWriteMode && NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {

            final Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            final NdefRecord[] record = new NdefRecord[1];

            record[0] = NdefRecord.createMime("x-gait/mnc",
                    CryptoHelper.mnemonic_to_bytes(UI.getText(mnemonicText)));

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
                    nfcTagsWritten.setText(String.valueOf(Integer.parseInt(UI.getText(nfcTagsWritten)) + 1));

                } else {
                    final NdefFormatable format = NdefFormatable.get(detectedTag);
                    if (format != null)
                        try {
                            format.connect();
                            format.format(message);
                            nfcTagsWritten.setText(String.valueOf(Integer.parseInt(UI.getText(nfcTagsWritten)) + 1));
                        } catch (final IOException e) {
                        }
                }
            } catch (final Exception e) {
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mnemonicDialog != null)
            mnemonicDialog.dismiss();
        if (nfcDialog != null)
            nfcDialog.dismiss();
    }

    @Override
    public void onBackPressed() {

        if (onSignUp != null) {
            mService.resetSignUp();
            onSignUp = null;
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
