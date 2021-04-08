package com.greenaddress.greenbits.ui.twofactor;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.greenaddress.gdk.GDKTwoFactorCall;
import com.greenaddress.greenapi.data.TwoFactorConfigData;
import com.greenaddress.greenapi.data.TwoFactorDetailData;
import com.greenaddress.greenbits.QrBitmap;
import com.greenaddress.greenbits.ui.LoggedActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;

import java.util.Formatter;
import java.util.Locale;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;



public class TwoFactorActivity extends LoggedActivity {

    private String mMethodName; // Current 2FA Method Name
    private String mMethod; // Current 2FA Method

    private Button mContinueButton;
    private TextView mPromptText;
    private EditText mPrefix;

    private boolean settingEmail; // setting email without enabling 2FA

    private PopupMethodResolver popupMethodResolver;
    private PopupCodeResolver popupCodeResolver;
    private TwoFactorConfigData twoFactorConfigData;
    private Disposable disposable;

    private void setView(final int layoutId, final int viewId) {
        setContentView(layoutId);
        mContinueButton = UI.find(this, R.id.continueButton);
        mPromptText = UI.find(this, R.id.prompt);
        mPrefix = UI.find(this, R.id.prefix);

        switch (mMethod) {
        case "reset":
            mContinueButton.setText(R.string.id_request_twofactor_reset);
            mPromptText.setText(R.string.id_resetting_your_twofactor_takes);
            break;
        case "dispute":
            mContinueButton.setText(R.string.id_dispute_twofactor_reset);
            break;
        case "undo_dispute":
            mContinueButton.setText(R.string.id_undo_2fa_dispute);
            mPromptText.setText(R.string.id_enter_the_email_for_which_you_want_to_undo);
            break;
        case "cancel":
            mContinueButton.setText(R.string.id_cancel_twofactor_reset);
            break;
        }
        UI.attachHideKeyboardListener(this, findViewById(viewId));
    }

    private String getTypeString(final String fmt, final String type) {
        return new Formatter().format(fmt, type).toString();
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitleBackTransparent();

        // 2FA method to localized description
        final Map<String, String> mLocalizedMap = UI.getTwoFactorLookup(getResources());
        mMethodName = getIntent().getStringExtra("method");
        mMethod = mMethodName.toLowerCase(Locale.US);
        settingEmail = getIntent().getBooleanExtra("settingEmail", false);
        final boolean mEnable = getIntent().getBooleanExtra("enable", true);
        try {
            twoFactorConfigData = getSession().getTwoFactorConfig();
        } catch (final Exception e) {
            UI.toast(this, R.string.id_operation_failure, Toast.LENGTH_SHORT);
            finishOnUiThread();
            return;
        }
        setTitle(getString(mEnable ? R.string.id_1s_twofactor_set_up : R.string.id_delete_s_twofactor,
                           mLocalizedMap.get(mMethod)));
        popupMethodResolver = new PopupMethodResolver(this);
        popupCodeResolver = new PopupCodeResolver(this);

        switch (mMethod) {
        case "reset":
            setTitle(R.string.id_request_twofactor_reset);
            showResetEmail(false, false);
            break;
        case "dispute":
            setTitle(R.string.id_dispute_twofactor_reset);
            showResetEmail(true, false);
            break;
        case "undo_dispute":
            setTitle(R.string.id_undo_2fa_dispute);
            showResetEmail(true, true);
            break;
        case "cancel":
            setTitle(R.string.id_cancel_twofactor_reset);
            cancelTwoFactor();
            break;
        case "email":
            if (mEnable)
                showProvideEmail();
            else
                disableTwoFactor("email");
            break;
        case "phone":
            if (mEnable)
                showProvidePhone();
            else
                disableTwoFactor("phone");
            break;
        case "sms":
            if (mEnable)
                showProvideSms();
            else
                disableTwoFactor("sms");
            break;
        case "gauth":
            if (mEnable)
                showProvideGAuth();
            else
                disableTwoFactor("gauth");
            break;
        }
    }

    private void showResetEmail(final boolean isDispute, final boolean isUndo) {
        final TextView detailsText = setupDetailsView(
            InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, "jane@example.com");
        mPrefix.setVisibility(View.GONE);
        final String type = getString(R.string.id_email_address);

        mPromptText.setText(getTypeString(UI.getText(mPromptText), type));
        mContinueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final String details = UI.getText(detailsText).trim();
                if (details.isEmpty())
                    return;
                if (!isValidEmail(details)) {
                    detailsText.setError(getString(R.string.id_not_a_valid_email_address));
                    return;
                }
                UI.disable(mContinueButton);
                resetTwoFactor(details, isDispute, isUndo);
            }
        });
    }

    private void showProvideEmail() {
        final TextView detailsText = setupDetailsView(
            InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, "jane@example.com");
        final String type = getString(R.string.id_email_address);
        mPrefix.setVisibility(View.GONE);

        mPromptText.setText(getTypeString(UI.getText(mPromptText), type));
        mContinueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final String details = UI.getText(detailsText).trim();
                if (details.isEmpty())
                    return;
                if (!isValidEmail(details)) {
                    detailsText.setError(getString(R.string.id_not_a_valid_email_address));
                    return;
                }
                UI.disable(mContinueButton);
                if (settingEmail) {
                    setEmail(details);
                } else {
                    enableTwoFactor("email", details);
                }

            }
        });
    }

    private void showProvidePhone() {
        final TextView detailsText = setupDetailsView(InputType.TYPE_CLASS_PHONE, "123456789");

        final String type = getString(R.string.id_phone_number);
        mPromptText.setText(getTypeString(UI.getText(mPromptText), type));
        mContinueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final String details = UI.getText(mPrefix).trim() + UI.getText(detailsText).trim();
                if (details.isEmpty())
                    return;
                if (!isValidPhoneNumber(details)) {
                    detailsText.setError(getString(R.string.id_invalid_phone_number_format));
                    return;
                }
                UI.disable(mContinueButton);
                enableTwoFactor("phone", details);
            }
        });
    }


    private void showProvideSms() {
        final TextView detailsText = setupDetailsView(InputType.TYPE_CLASS_PHONE, "123456789");

        final String type = getString(R.string.id_phone_number);
        mPromptText.setText(getTypeString(UI.getText(mPromptText), type));
        mContinueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final String details = UI.getText(mPrefix).trim() + UI.getText(detailsText).trim();
                if (details.isEmpty())
                    return;
                if (!isValidPhoneNumber(details)) {
                    detailsText.setError(getString(R.string.id_invalid_phone_number_format));
                    return;
                }
                UI.disable(mContinueButton);
                enableTwoFactor("sms", details);
            }
        });
    }

    private void showProvideGAuth() {
        setView(R.layout.activity_two_factor_3_gauth_details, R.id.activity_two_factor_3_gauth_details);
        final ImageView imageView = UI.find(this, R.id.imageView);
        final TextView textCode = UI.find(this, R.id.textCode);
        final LinearLayout layoutCode = UI.find(this, R.id.layoutCode);
        final String gauthUrl = twoFactorConfigData.getGauth().getData();

        try {
            final BitmapDrawable bd =
                new BitmapDrawable(getResources(), new QrBitmap(gauthUrl, getResources().getColor(
                                                                    R.color.white)).getQRCode());
            bd.setFilterBitmap(false);
            imageView.setImageDrawable(bd);
        }catch (final Exception e) {
            e.printStackTrace();
        }

        final String gauthCode = gauthUrl.split("=")[1];
        textCode.setText(gauthCode);

        layoutCode.setOnClickListener(
            v -> {
            final ClipboardManager clipboard = (ClipboardManager)
                                               getSystemService(Context.CLIPBOARD_SERVICE);
            final ClipData clip = ClipData.newPlainText("data", gauthCode);
            clipboard.setPrimaryClip(clip);
            UI.toast(TwoFactorActivity.this, R.string.id_be_aware_other_apps_can_read_or, Toast.LENGTH_LONG);
        }
            );

        mContinueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                //final Map<String, String> twoFacData = make2FAData("proxy", proxyCode);
                mContinueButton.setEnabled(false);
                try {
                    enableTwoFactor("gauth", gauthUrl);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private TextView setupDetailsView(final int inputType, final String hint) {
        setView(R.layout.activity_two_factor_3_provide_details, R.id.activity_two_factor_3_provide_details);

        final TextView detailsText = UI.find(this, R.id.details);
        detailsText.setInputType(inputType);
        detailsText.setHint(hint);

        final InputMethodManager imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        // Show the keyboard; this workaround is needed due to Android bugs
        detailsText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    detailsText.postDelayed(() -> {
                        detailsText.requestFocus();
                        imm.showSoftInput(detailsText, 0);
                    }, 100);
                }
            }
        });
        return detailsText;
    }

    static boolean isValidPhoneNumber(final String phoneNumber) {
        if (phoneNumber.startsWith("+") && phoneNumber.length() > 7 && phoneNumber.length() < 20) {
            final String stripped = phoneNumber.replaceAll("^\\+0", "").replaceAll("^\\+", "")
                                    .replace(" ", "").replace("(", "").replace(")", "");
            return !stripped.startsWith("00") && stripped.matches("\\d+?");
        }
        return false;
    }

    static boolean isValidEmail(final String email) {
        return email.matches(".+@.+\\..+");
    }

    public void setEmail(String data) {
        disposable = Observable.just(getSession())
                     .subscribeOn(Schedulers.computation())
                     .map((session) -> {
            final TwoFactorDetailData twoFactorDetail = new TwoFactorDetailData();
            twoFactorDetail.setData(data);
            twoFactorDetail.setEnabled(false);
            twoFactorDetail.setConfirmed(true);
            session.changeSettingsTwoFactor("email", twoFactorDetail).resolve(popupMethodResolver, popupCodeResolver);
            return session;
        })
                     .observeOn(AndroidSchedulers.mainThread())
                     .subscribe((session) -> {
            UI.toast(this, "Email set", Toast.LENGTH_LONG);
            setEnableDisableResult(false);
            finishOnUiThread();
        }, (final Throwable e) -> {
            UI.toast(this, e.getMessage(), Toast.LENGTH_LONG);
        });
    }

    public void enableTwoFactor(final String method, final String data) {
        if (twoFactorConfigData.getEnabledMethods().contains(method.toLowerCase(Locale.US))) {
            UI.toast(this, "Two factor just enabled", Toast.LENGTH_LONG);
            finishOnUiThread();
            return;
        }

        disposable = Observable.just(getSession())
                     .subscribeOn(Schedulers.computation())
                     .map((session) -> {
            final TwoFactorDetailData twoFactorDetail = new TwoFactorDetailData();
            twoFactorDetail.setEnabled(true);
            twoFactorDetail.setData(data);
            twoFactorDetail.setConfirmed(true);
            session.changeSettingsTwoFactor(method, twoFactorDetail).resolve(popupMethodResolver, popupCodeResolver);
            return session;
        })
                     .observeOn(AndroidSchedulers.mainThread())
                     .subscribe((session) -> {
            UI.toast(this, "Two factor enabled", Toast.LENGTH_LONG);
            setEnableDisableResult(true);
            finishOnUiThread();
        }, (final Throwable e) -> {
            UI.toast(this, e.getMessage(), Toast.LENGTH_LONG);
            finishOnUiThread();
            return;
        });
    }

    private void setEnableDisableResult(final boolean enabled)
    {
        final Intent intent = getIntent();
        intent.putExtra("method",mMethodName);
        intent.putExtra("enable",enabled);
        setResult(Activity.RESULT_OK, intent);
    }

    private void disableTwoFactor(final String method) {
        if (!twoFactorConfigData.getEnabledMethods().contains(method.toLowerCase(Locale.US))) {
            UI.toast(this, R.string.id_your_wallet_is_not_yet_fully, Toast.LENGTH_LONG);
            finishOnUiThread();
            return;
        }

        disposable = Observable.just(getSession())
                     .subscribeOn(Schedulers.computation())
                     .map((session) -> {
            final TwoFactorDetailData twoFactorDetail = twoFactorConfigData.getMethod(method);
            twoFactorDetail.setEnabled(false);
            if (method.equals("email")) {
                // FIXME:  GDK currently doesn't correctly handle email disabling
                // unless we set confirmed: false while disabling, so work around
                // it here
                twoFactorDetail.setConfirmed(false);
            }
            session.changeSettingsTwoFactor(method, twoFactorDetail).resolve(popupMethodResolver, popupCodeResolver);
            return session;
        }).observeOn(AndroidSchedulers.mainThread())
                     .subscribe((session) -> {
            UI.toast(this, "Two factor disabled", Toast.LENGTH_LONG);
            setEnableDisableResult(false);
            finishOnUiThread();
        }, (final Throwable e) -> {
            UI.toast(this, e.getMessage(), Toast.LENGTH_LONG);
        });
    }

    public void resetTwoFactor(final String email, final Boolean isDispute, final Boolean isUndo) {
        if (twoFactorConfigData.getEnabledMethods().isEmpty()) {
            UI.toast(this, R.string.id_your_wallet_is_not_yet_fully, Toast.LENGTH_LONG);
            finishOnUiThread();
            return;
        }

        disposable = Observable.just(getSession())
                     .subscribeOn(Schedulers.computation())
                     .map((session) -> {
             GDKTwoFactorCall twoFactorCall;
             if(isDispute && isUndo){
                 twoFactorCall = getSession().twoFactorUndoDispute(email);
             }else{
                 twoFactorCall = getSession().twoFactorReset(email, isDispute);
             }
             twoFactorCall.resolve(popupMethodResolver, popupCodeResolver);
             return session;
        }).observeOn(AndroidSchedulers.mainThread())
                     .subscribe((session) -> {
            if(isDispute && isUndo) {
                UI.toast(this, R.string.id_undo_2fa_dispute, Toast.LENGTH_LONG);
            }else{
                UI.toast(this, R.string.id_request_twofactor_reset, Toast.LENGTH_LONG);
            }
            final Intent intent = getIntent();
            intent.putExtra("method","reset");
            intent.putExtra("enable", true);
            setResult(Activity.RESULT_OK, intent);
            finishOnUiThread();
        }, (final Throwable e) -> {
            UI.toast(this, e.getMessage(), Toast.LENGTH_LONG);
        });
    }

    public void cancelTwoFactor() {
        if (twoFactorConfigData.getEnabledMethods().isEmpty()) {
            UI.toast(this, R.string.id_your_wallet_is_not_yet_fully, Toast.LENGTH_LONG);
            finishOnUiThread();
            return;
        }

        disposable = Observable.just(getSession())
                     .subscribeOn(Schedulers.computation())
                     .map((session) -> {
            final GDKTwoFactorCall twoFactorCall = getSession().twofactorCancelReset();
            twoFactorCall.resolve(popupMethodResolver, popupCodeResolver);
            return session;
        })
                     .observeOn(AndroidSchedulers.mainThread())
                     .subscribe((session) -> {
            UI.toast(this, R.string.id_cancel_twofactor_reset, Toast.LENGTH_LONG);
            setResult(Activity.RESULT_OK);
            finishOnUiThread();
        }, (final Throwable e) -> {
            UI.toast(this, e.getMessage(), Toast.LENGTH_LONG);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (disposable != null)
            disposable.dispose();
        if (popupMethodResolver != null)
            popupMethodResolver.dismiss();
        if (popupCodeResolver != null)
            popupCodeResolver.dismiss();
    }

    @Override
    public void onBackPressed()
    {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
