package com.greenaddress.greenbits.ui;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.dd.CircularProgressButton;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.greenaddress.greenbits.KeyStoreAES;

public class PinSaveActivity extends GaActivity {

    private static final int ACTIVITY_REQUEST_CODE = 1;
    private static final String NEW_PIN_MNEMONIC = "com.greenaddress.greenbits.NewPinMnemonic";

    private EditText mPinText;
    private Button mSkipButton;
    private CircularProgressButton mSaveButton;

    private Dialog mVerifyDialog;

    static public Intent createIntent(final Context ctx, final String mnemonic) {
        final Intent intent = new Intent(ctx, PinSaveActivity.class);
        intent.putExtra(NEW_PIN_MNEMONIC, mnemonic);
        return intent;
    }

    private void setPin(final String pin, final boolean isNative) {

        if (pin.length() < 4) {
            shortToast(R.string.err_pin_save_wrong_length);
            return;
        }

        hideKeyboardFrom(mPinText);
        mPinText.setEnabled(false);

        mSaveButton.setIndeterminateProgressMode(true);
        mSaveButton.setProgress(50);
        UI.hide(mSkipButton);

        final String mnemonic = getIntent().getStringExtra(NEW_PIN_MNEMONIC);
        Futures.addCallback(mService.setPin(mnemonic, pin),
                new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(final Void result) {
                        setResult(RESULT_OK);
                        if (!isNative && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            // The user has set a non-native PIN.
                            // In case they already had a native PIN they are overriding,
                            // blank the native value so future logins don't detect it.
                            // FIXME: Requiring M or higher is required because otherwise this crashes @ android < 21
                            // and native is not available before M anyway
                            // java.lang.VerifyError: com/greenaddress/greenbits/KeyStoreAES
                            KeyStoreAES.wipePIN(mService);
                        }
                        finishOnUiThread();
                    }

                    @Override
                    public void onFailure(final Throwable t) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                mSaveButton.setProgress(0);
                                mPinText.setEnabled(true);
                                UI.show(mSkipButton);
                            }
                        });
                    }
                }, mService.getExecutor());
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {

        if (requestCode == ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Challenge completed, proceed with using cipher
            tryEncrypt();
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void tryEncrypt() {
        try {
            setPin(KeyStoreAES.tryEncrypt(mService), true);
        } catch (final KeyStoreAES.RequiresAuthenticationScreen e) {
            KeyStoreAES.showAuthenticationScreen(this);
        } catch (final KeyStoreAES.KeyInvalidated e) {
            toast(getString(R.string.problemWithKey, e.getMessage()));
        }
    }

    @Override
    protected int getMainViewId() { return R.layout.activity_pin_save; }

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {

        mPinText = UI.find(this, R.id.pinSaveText);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                KeyStoreAES.createKey(true);

                final CheckBox nativeAuthCB = UI.find(this, R.id.useNativeAuthentication);
                UI.show(nativeAuthCB);
                nativeAuthCB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(final CompoundButton compoundButton, final boolean isChecked) {
                        if (isChecked)
                            tryEncrypt();
                    }
                });

            } catch (final RuntimeException e) {
                // lock not set, simply don't show native options
            }
        }

        mPinText.setOnEditorActionListener(
                UI.getListenerRunOnEnter(new Runnable() {
                    public void run() {
                        onSaveNonNativePin();
                    }
                }));

        mSaveButton = (CircularProgressButton) UI.mapClick(this, R.id.pinSaveButton, new View.OnClickListener() {
            public void onClick(final View v) {
                onSaveNonNativePin();
            }
        });

        mSkipButton = (Button) UI.mapClick(this, R.id.pinSkipButton, new View.OnClickListener() {
            public void onClick(final View v) {
                setResult(RESULT_CANCELED); // Skip
                finish();
            }
        });
    }

    @Override
    public void onPauseWithService() {
        mVerifyDialog = UI.dismiss(this, mVerifyDialog);
    }

    private void onSaveNonNativePin() {
        final String currentPin = UI.getText(mPinText);

        final View v = getLayoutInflater().inflate(R.layout.dialog_btchip_pin, null, false);

        UI.hide(UI.find(v, R.id.btchipPinPrompt));
        final TextView newPin = UI.find(v, R.id.btchipPINValue);

        mVerifyDialog = UI.popup(this, R.string.verify_your_pin, R.string.signupContinueButtonText, R.string.cancel)
                .customView(v, true)
                .autoDismiss(false)
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(final MaterialDialog dialog, final DialogAction which) {
                        UI.dismiss(null, PinSaveActivity.this.mVerifyDialog);
                    }
                })
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(final MaterialDialog dialog, final DialogAction which) {
                        if (UI.getText(newPin).equals(currentPin)) {
                            UI.dismiss(null, PinSaveActivity.this.mVerifyDialog);
                            setPin(currentPin, false);
                            return;
                        }
                        UI.toast(PinSaveActivity.this, R.string.pins_dont_match, Toast.LENGTH_SHORT);
                    }
                }).build();
        UI.mapEnterToPositive(mVerifyDialog, R.id.btchipPINValue);
        mVerifyDialog.show();
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
}
