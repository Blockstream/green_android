package com.greenaddress.greenbits.ui.onboarding;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.greenaddress.greenapi.data.PinData;
import com.greenaddress.greenbits.AuthenticationHandler;
import com.greenaddress.greenbits.ui.GaActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;
import com.greenaddress.greenbits.ui.authentication.PinFragment;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;



public class PinSaveActivity extends GaActivity implements PinFragment.OnPinListener {

    private static final String NEW_PIN_MNEMONIC = "com.greenaddress.greenbits.NewPinMnemonic";

    private PinFragment mPinFragment;
    private PinFragment mPinFragmentVerify;
    private TextView mTitleText;
    private Disposable saveDisposable;

    static public Intent createIntent(final Context ctx, final String mnemonic) {
        final Intent intent = new Intent(ctx, PinSaveActivity.class);
        intent.putExtra(NEW_PIN_MNEMONIC, mnemonic);
        return intent;
    }

    private void setPin(final String pin) {

        if (pin.length() < 4) {
            UI.toast(this, R.string.id_pin_has_to_be_between_4_and_15, Toast.LENGTH_SHORT);
            return;
        }

        mPinFragment.setEnabled(false);
        startLoading();

        final Intent intent = new Intent(this, SecurityActivity.class);
        intent.putExtra("from_onboarding",true);
        final String mnemonic = getIntent().getStringExtra(NEW_PIN_MNEMONIC);

        saveDisposable = Observable.just(getSession())
                         .observeOn(Schedulers.computation())
                         .map((session) -> {
            return session.setPin(mnemonic, pin, "default");
        })
                         .observeOn(AndroidSchedulers.mainThread())
                         .subscribe((pinData) -> {
            AuthenticationHandler.setPin(pinData, pin.length() == 6, AuthenticationHandler.getNewAuth(this));
            getSession().setPinJustSaved(true);
            setResult(RESULT_OK);
            stopLoading();
            startActivity(intent);
        }, (e) -> {
            stopLoading();
            mPinFragment.setEnabled(true);
            UI.popup(this, R.string.id_warning).content(e.getLocalizedMessage()).show();
        });
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (saveDisposable != null)
            saveDisposable.dispose();
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
