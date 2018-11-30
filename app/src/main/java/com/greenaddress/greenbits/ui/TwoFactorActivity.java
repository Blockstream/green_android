package com.greenaddress.greenbits.ui;

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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.greenaddress.gdk.GDKTwoFactorCall;
import com.greenaddress.greenapi.data.TwoFactorConfigData;
import com.greenaddress.greenapi.data.TwoFactorDetailData;
import com.greenaddress.greenbits.QrBitmap;

import java.util.Formatter;
import java.util.Locale;
import java.util.Map;

public class TwoFactorActivity extends LoggedActivity {

    private String mMethodName; // Current 2FA Method Name
    private String mMethod; // Current 2FA Method
    private boolean mEnable;
    private Map<String, String> mLocalizedMap; // 2FA method to localized description

    private Button mContinueButton;
    private TextView mPromptText;

    private TwoFactorConfigData twoFactorConfigData;

    private void setView(final int id) {
        setContentView(id);
        mContinueButton = UI.find(this, R.id.continueButton);
        mPromptText = UI.find(this, R.id.prompt);
        switch (mMethod) {
        case "reset":
            mContinueButton.setText(R.string.id_request_twofactor_reset);
            break;
        case "dispute":
            mContinueButton.setText(R.string.id_dispute_twofactor_reset);
            showResetEmail(true);
            break;
        case "cancel":
            mContinueButton.setText(R.string.id_cancel_twofactor_reset);
            break;
        }
    }

    private String getTypeString(final String fmt, final String type) {
        return new Formatter().format(fmt, type).toString();
    }

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {
        setTitleBackTransparent();

        if (mService.getModel().getTwoFactorConfig() == null) {
            finish();
            return;
        }

        mLocalizedMap = UI.getTwoFactorLookup(getResources());
        mMethodName = getIntent().getStringExtra("method");
        mMethod = mMethodName.toLowerCase(Locale.US);
        mEnable = getIntent().getBooleanExtra("enable", true);
        twoFactorConfigData = mService.getModel().getTwoFactorConfigDataObservable().getTwoFactorConfigData();
        setTitle(getTypeString(getTitle().toString(), mLocalizedMap.get(mMethod)));

        switch (mMethod) {
        case "reset":
            setTitle(R.string.id_request_twofactor_reset);
            showResetEmail(false);
            break;
        case "dispute":
            setTitle(R.string.id_dispute_twofactor_reset);
            showResetEmail(true);
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

    private void showResetEmail(final boolean isDispute) {
        final TextView detailsText = setupDetailsView(
            InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, "jane@example.com");

        final String type = getString(R.string.id_email_address);

        mPromptText.setText(getTypeString(UI.getText(mPromptText), type));
        mContinueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final String details = UI.getText(detailsText).trim();
                if (details.isEmpty())
                    return;
                UI.disable(mContinueButton);
                resetTwoFactor(details, isDispute);
            }
        });
    }

    private void showProvideEmail() {
        final TextView detailsText = setupDetailsView(
            InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, "jane@example.com");
        final String type = getString(R.string.id_email_address);

        mPromptText.setText(getTypeString(UI.getText(mPromptText), type));
        mContinueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final String details = UI.getText(detailsText).trim();
                if (details.isEmpty())
                    return;
                UI.disable(mContinueButton);
                enableTwoFactor("email", details);
            }
        });
    }

    private void showProvidePhone() {
        final TextView detailsText = setupDetailsView(InputType.TYPE_CLASS_PHONE, "+123456789");

        final String type = getString(R.string.id_phone_number);
        mPromptText.setText(getTypeString(UI.getText(mPromptText), type));
        mContinueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final String details = UI.getText(detailsText).trim();
                if (details.isEmpty())
                    return;
                if (!isValidPhoneNumber(details)) {
                    toast(R.string.id_invalid_phone_number_format);
                    return;
                }
                UI.disable(mContinueButton);
                enableTwoFactor("phone", details);
            }
        });
    }


    private void showProvideSms() {
        final TextView detailsText = setupDetailsView(InputType.TYPE_CLASS_PHONE, "+123456789");

        final String type = getString(R.string.id_phone_number);
        mPromptText.setText(getTypeString(UI.getText(mPromptText), type));
        mContinueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final String details = UI.getText(detailsText).trim();
                if (details.isEmpty())
                    return;
                if (!isValidPhoneNumber(details)) {
                    toast(R.string.id_invalid_phone_number_format);
                    return;
                }
                UI.disable(mContinueButton);
                enableTwoFactor("sms", details);
            }
        });
    }

    private void showProvideGAuth() {
        setView(R.layout.activity_two_factor_3_gauth_details);
        final ImageView imageView = UI.find(this, R.id.imageView);
        final TextView textCode = UI.find(this, R.id.textCode);
        final LinearLayout layoutCode = UI.find(this, R.id.layoutCode);

        final String gauthUrl = mService.getModel().getTwoFactorConfig().getGauth().getData();
        final BitmapDrawable bd =
            new BitmapDrawable(getResources(), new QrBitmap(gauthUrl, getResources().getColor(
                                                                R.color.white)).getQRCode());
        bd.setFilterBitmap(false);
        imageView.setImageDrawable(bd);

        final String gauthCode = gauthUrl.split("=")[1];
        textCode.setText(gauthCode);

        layoutCode.setOnClickListener(
            v -> {
            final ClipboardManager clipboard = (ClipboardManager)
                                               getSystemService(Context.CLIPBOARD_SERVICE);
            final ClipData clip = ClipData.newPlainText("data", gauthCode);
            clipboard.setPrimaryClip(clip);
            TwoFactorActivity.this.toast(R.string.id_be_aware_other_apps_can_read_or);
        }
            );

        mContinueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                //final Map<String, String> twoFacData = mService.make2FAData("proxy", proxyCode);
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
        setView(R.layout.activity_two_factor_3_provide_details);

        final TextView detailsText = UI.find(this, R.id.details);
        detailsText.setInputType(inputType);
        detailsText.setHint(hint);

        final InputMethodManager imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        // Show the keyboard; this workaround is needed due to Android bugs
        detailsText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    detailsText.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            detailsText.requestFocus();
                            imm.showSoftInput(detailsText, 0);
                        }
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

    public void enableTwoFactor(final String method, final String data) {
        if (twoFactorConfigData.getEnabledMethods().contains(method.toLowerCase(Locale.US))) {
            UI.toast(this, "Two factor just enabled", Toast.LENGTH_LONG);
            finishOnUiThread();
            return;
        }

        mService.getExecutor().execute(() -> {
            try {
                final TwoFactorDetailData twoFactorDetail = new TwoFactorDetailData();
                twoFactorDetail.setEnabled(true);
                twoFactorDetail.setData(data);
                twoFactorDetail.setConfirmed(true);
                final GDKTwoFactorCall twoFactorCall =
                    mService.getSession().changeSettingsTwoFactor(this, method, twoFactorDetail);
                twoFactorCall.resolve(new PopupMethodResolver(this), new PopupCodeResolver(this));
                UI.toast(this, "Two factor enabled", Toast.LENGTH_LONG);
                setEnableDisableResult(true);
            } catch (final Exception e) {
                e.printStackTrace();
                UI.toast(this, UI.i18n(getResources(), e.getMessage()), Toast.LENGTH_LONG);
            }
            finishOnUiThread();
        });
    }

    private void setEnableDisableResult(boolean enabled)
    {
        Intent intent = getIntent();
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

        mService.getExecutor().execute(() -> {
            try {
                final TwoFactorDetailData twoFactorDetail = twoFactorConfigData.getMethod(method);
                twoFactorDetail.setEnabled(false);
                if (method.equals("email")) {
                    // FIXME:  GDK currently doesn't correctly handle email disabling
                    // unless we set confirmed: false while disabling, so work around
                    // it here
                    twoFactorDetail.setConfirmed(false);
                }
                final GDKTwoFactorCall twoFactorCall =
                    mService.getSession().changeSettingsTwoFactor(this, method, twoFactorDetail);
                twoFactorCall.resolve(new PopupMethodResolver(this), new PopupCodeResolver(this));
                UI.toast(this, "Two factor disabled", Toast.LENGTH_LONG);
                setEnableDisableResult(false);
            } catch (final Exception e) {
                e.printStackTrace();
                UI.toast(this, UI.i18n(getResources(),e.getMessage()), Toast.LENGTH_LONG);
            }
            finishOnUiThread();
        });
    }

    public void resetTwoFactor(final String email, final Boolean isDispute) {
        if (twoFactorConfigData.getEnabledMethods().isEmpty()) {
            UI.toast(this, R.string.id_your_wallet_is_not_yet_fully, Toast.LENGTH_LONG);
            finishOnUiThread();
            return;
        }

        mService.getExecutor().execute(() -> {
            try {
                final GDKTwoFactorCall twoFactorCall = mService.getSession().twoFactorReset(this, email, isDispute);
                twoFactorCall.resolve(new PopupMethodResolver(this), new PopupCodeResolver(this));
                UI.toast(this, R.string.id_request_twofactor_reset, Toast.LENGTH_LONG);
                Intent intent = getIntent();
                intent.putExtra("method","reset");
                intent.putExtra("enable", true);
                setResult(Activity.RESULT_OK, intent);
            } catch (final Exception e) {
                e.printStackTrace();
                UI.toast(this, UI.i18n(getResources(),e.getMessage()), Toast.LENGTH_LONG);
            }
            finishOnUiThread();
        });
    }

    public void cancelTwoFactor() {
        if (twoFactorConfigData.getEnabledMethods().isEmpty()) {
            UI.toast(this, R.string.id_your_wallet_is_not_yet_fully, Toast.LENGTH_LONG);
            finishOnUiThread();
            return;
        }

        mService.getExecutor().execute(() -> {
            try {
                final GDKTwoFactorCall twoFactorCall = mService.getSession().twofactorCancelReset(this);
                twoFactorCall.resolve(new PopupMethodResolver(this), new PopupCodeResolver(this));
                UI.toast(this, R.string.id_cancel_twofactor_reset, Toast.LENGTH_LONG);
                setResult(Activity.RESULT_OK);
            } catch (final Exception e) {
                e.printStackTrace();
                UI.toast(this, UI.i18n(getResources(),e.getMessage()), Toast.LENGTH_LONG);
            }
            finishOnUiThread();
        });
    }

    @Override
    public void onBackPressed()
    {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
