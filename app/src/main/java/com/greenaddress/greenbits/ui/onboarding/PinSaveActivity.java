package com.greenaddress.greenbits.ui.onboarding;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.greenaddress.greenbits.AuthenticationHandler;
import com.greenaddress.greenbits.ui.GaActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;
import com.greenaddress.greenbits.ui.authentication.PinFragment;

public class PinSaveActivity extends GaActivity implements PinFragment.OnPinListener {

    private static final String NEW_PIN_MNEMONIC = "com.greenaddress.greenbits.NewPinMnemonic";

    private PinFragment mPinFragment;
    private PinFragment mPinFragmentVerify;
    private TextView mTitleText;

    static public Intent createIntent(final Context ctx, final String mnemonic) {
        final Intent intent = new Intent(ctx, PinSaveActivity.class);
        intent.putExtra(NEW_PIN_MNEMONIC, mnemonic);
        return intent;
    }

    private void setPin(final String pin) {

        if (pin.length() < 4) {
            shortToast(R.string.id_pin_has_to_be_between_4_and_15);
            return;
        }

        mPinFragment.setEnabled(false);
        startLoading();

        final Intent intent = new Intent(this, SecurityActivity.class);
        intent.putExtra("from_onboarding",true);

        final String mnemonic = getIntent().getStringExtra(NEW_PIN_MNEMONIC);
        final ListenableFuture<Void> future = getGAApp().getExecutor().submit(() -> {
            getConnectionManager().setPin(mnemonic, pin, AuthenticationHandler.getNewAuth(this));
            return null;
        });
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                setResult(RESULT_OK);
                stopLoading();
                startActivity(intent);
            }

            @Override
            public void onFailure(final Throwable t) {
                runOnUiThread(() -> {
                    stopLoading();
                    mPinFragment.setEnabled(true);
                    UI.popup(PinSaveActivity.this, R.string.id_warning).content(t.getLocalizedMessage()).show();
                });
            }
        }, getGAApp().getExecutor());
    }

    @Override
    protected int getMainViewId() { return R.layout.activity_pin_save; }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final String mnemonic = getIntent().getStringExtra(NEW_PIN_MNEMONIC);
        if (isHexSeed(mnemonic))
            goToTabbedMainActivity();

        setAppNameTitle();
        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(android.R.color.transparent));
        mTitleText = UI.find(this, R.id.name);

        mPinFragment = new PinFragment();
        getSupportFragmentManager().beginTransaction()
        .add(R.id.fragment_container, mPinFragment).commit();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    private void onSaveNonNativePin() {
        mTitleText.setText(R.string.id_verify_your_pin);
        mPinFragmentVerify = new PinFragment();
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
            setPin(pin);
            return;
        }
        mPinFragmentVerify.clear();
        UI.toast(this, R.string.id_pins_do_not_match_please_try, Toast.LENGTH_SHORT);
    }

    @Override
    public void onPinBackPressed() {}

    @Override
    public void onBackPressed() {}

}
