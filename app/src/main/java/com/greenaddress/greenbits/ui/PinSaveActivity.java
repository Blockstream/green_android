package com.greenaddress.greenbits.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.greenaddress.greenbits.KeyStoreAES;

public class PinSaveActivity extends GaActivity implements PinFragment.OnPinListener {

    private static final int ACTIVITY_REQUEST_CODE = 1;
    private static final String NEW_PIN_MNEMONIC = "com.greenaddress.greenbits.NewPinMnemonic";

    private PinFragment mPinFragment;
    private PinFragment mPinFragmentVerify;
    private TextView mTitleText;
    private CheckBox mNativeAuthCB;

    static public Intent createIntent(final Context ctx, final String mnemonic) {
        final Intent intent = new Intent(ctx, PinSaveActivity.class);
        intent.putExtra("skip_visible", true);
        intent.putExtra(NEW_PIN_MNEMONIC, mnemonic);
        return intent;
    }

    private void setPin(final String pin, final boolean isNative) {

        if (pin.length() < 4) {
            shortToast(R.string.id_pin_has_to_be_between_4_and_15);
            return;
        }

        mPinFragment.setEnabled(false);
        startLoading();

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
                stopLoading();
                finishOnUiThread();
            }

            @Override
            public void onFailure(final Throwable t) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        stopLoading();
                        mPinFragment.setEnabled(true);
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
            toast(getString(R.string.id_problem_with_key_1s, e.getMessage()));
        }
    }

    @Override
    protected int getMainViewId() { return R.layout.activity_pin_save; }

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {
        final String mnemonic = getIntent().getStringExtra(NEW_PIN_MNEMONIC);
        if (isHexSeed(mnemonic))
            goToTabbedMainActivity();

        setAppNameTitle();
        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(android.R.color.transparent));
        mTitleText = UI.find(this, R.id.name);

        mPinFragment = new PinFragment();
        final Bundle bundle = new Bundle();
        bundle.putBoolean("skip_visible", getIntent().getBooleanExtra("skip_visible", false));
        mPinFragment.setArguments(bundle);
        getSupportFragmentManager().beginTransaction()
        .add(R.id.fragment_container, mPinFragment).commit();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                KeyStoreAES.createKey(true, mService);

                mNativeAuthCB = UI.find(this, R.id.useNativeAuthentication);
                UI.show(mNativeAuthCB);
                mNativeAuthCB.setOnCheckedChangeListener((compoundButton, isChecked) -> {
                    if (isChecked)
                        tryEncrypt();
                });

            } catch (final RuntimeException e) {
                // lock not set, simply don't show native options
            }
        }
    }

    private void onSaveNonNativePin() {
        mTitleText.setText(R.string.id_verify_your_pin);
        mPinFragmentVerify = new PinFragment();
        final Bundle bundle = new Bundle();
        bundle.putBoolean("skip_visible", false);
        mPinFragmentVerify.setArguments(bundle);
        if (mNativeAuthCB != null)
            mNativeAuthCB.setVisibility(View.INVISIBLE);
        getSupportFragmentManager().beginTransaction()
        .replace(R.id.fragment_container, mPinFragmentVerify).commit();
    }

    @Override
    public void onPinInserted(final String pin) {
        if (mPinFragmentVerify == null) {
            onSaveNonNativePin();
            return;
        }
        if (mPinFragment.getPin().equals(pin)) {
            setPin(pin, false);
            return;
        }
        mPinFragmentVerify.clear();
        UI.toast(this, R.string.id_pins_do_not_match_please_try, Toast.LENGTH_SHORT);
    }

    @Override
    public void onPinBackPressed() {}

}
