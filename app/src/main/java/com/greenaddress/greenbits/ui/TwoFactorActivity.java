package com.greenaddress.greenbits.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.greenaddress.greenbits.QrBitmap;
import com.google.common.collect.ImmutableMap;

import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TwoFactorActivity extends GaActivity {

    private String mMethod; // Current 2FA Method
    private Map<String, String> mLocalizedMap; // 2FA method to localized description

    private Button mContinueButton;
    private TextView mPromptText;
    private ProgressBar mProgressBar;
    private EditText mCodeText;

    private void setView(final int id) {
        setContentView(id);
        mContinueButton = UI.find(this, R.id.continueButton);
        mPromptText = UI.find(this, R.id.prompt);
        mProgressBar = UI.find(this, R.id.progressBar);
        mCodeText = UI.find(this, R.id.code);
    }

    private String getTypeString(final String fmt, final String type) {
        return new Formatter().format(fmt, type).toString();
    }

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {

        if (mService.getTwoFactorConfig() == null) {
            finish();
            return;
        }

        mMethod = getIntent().getStringExtra("method");
        mLocalizedMap = UI.getTwoFactorLookup(getResources());

        setTitle(getTypeString(getTitle().toString(), mLocalizedMap.get(mMethod)));

        final List<String> enabledMethods = mService.getEnabledTwoFactorMethods();

        if (enabledMethods.size() > 1) {
            // Multiple 2FA options enabled - Allow the user to choose
            setView(R.layout.activity_two_factor_1_choose);

            final RadioGroup group = UI.find(this, R.id.radioGroup);
            group.removeViews(0, group.getChildCount());

            for (int i = 0; i < enabledMethods.size(); ++i) {
                final RadioButton b = new RadioButton(TwoFactorActivity.this);
                b.setText(mLocalizedMap.get(enabledMethods.get(i)));
                b.setId(i);
                group.addView(b);
            }

            group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(final RadioGroup group, final int checkedId) {
                    mContinueButton.setEnabled(true);
                }
            });

            mContinueButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    final int checked = group.getCheckedRadioButtonId();
                    showProvideAuthCode(2, enabledMethods.get(checked));
                }
            });
        } else if (enabledMethods.size() == 1) {
            // just one 2FA enabled - go straight to code verification
            showProvideAuthCode(1, enabledMethods.get(0));
        } else
            // no 2FA enabled - go straight to 2FA details
            if (mMethod.equals("gauth"))
                showGauthDetails(1, 1, null);
            else
                showProvideDetails(1, 2, null);
    }

    private Map<String, String> make2FAData(final String method, final String code) {
        if (code == null)
            return new HashMap<String, String>();
        return ImmutableMap.of("method", method, "code", code);
    }

    private void showProvideDetails(final int stepNum, final int numSteps, final String proxyCode) {
        setView(R.layout.activity_two_factor_3_provide_details);

        final boolean isEmail = mMethod.equals("email");
        final int resId = isEmail ? R.string.emailAddress : R.string.phoneNumber;
        final String type = getResources().getString(resId);

        mPromptText.setText(getTypeString(UI.getText(mPromptText), type));
        if (!isEmail)
            UI.hide((View) UI.find(this, R.id.emailNotices));

        mProgressBar.setProgress(stepNum);
        mProgressBar.setMax(numSteps);

        final TextView detailsText = UI.find(this, R.id.details);
        mContinueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final String details = UI.getText(detailsText).trim();
                if (details.isEmpty())
                    return;
                UI.disable(mContinueButton);
                final Map<String, String> twoFacData = make2FAData("proxy", proxyCode);
                CB.after(mService.initEnableTwoFac(mMethod, details, twoFacData),
                         new CB.Toast<Boolean>(TwoFactorActivity.this, mContinueButton) {
                    @Override
                    public void onSuccess(final Boolean result) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                showProvideConfirmationCode(stepNum + 1, numSteps);
                            }
                        });
                    }
                });
            }
        });
    }

    private void showProvideAuthCode(final int stepNum, final String method) {
        final int numSteps = stepNum + (mMethod.equals("gauth") ? 1 : 2);

        final String localizedName = mLocalizedMap.get(method);
        if (!method.equals("gauth"))
            mService.requestTwoFacCode(method, "enable_2fa",
                                       ImmutableMap.of("method", mMethod));

        setView(R.layout.activity_two_factor_2_4_provide_code);

        final TextView descriptionText = UI.find(this, R.id.description);
        descriptionText.setText(R.string.twoFacProvideAuthCodeDescription);
        mPromptText.setText(getTypeString(UI.getText(mPromptText), localizedName));
        mProgressBar.setProgress(stepNum);
        mProgressBar.setMax(numSteps);

        mContinueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                mContinueButton.setEnabled(false);
                final Map<String, String> data = make2FAData(method, UI.getText(mCodeText).trim());
                CB.after(mService.requestTwoFacCode("proxy", mMethod, data),
                         new CB.Toast<Object>(TwoFactorActivity.this, mContinueButton) {
                    @Override
                    public void onSuccess(final Object proxyCode) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                if (mMethod.equals("gauth"))
                                    showGauthDetails(stepNum + 1, numSteps, (String) proxyCode);
                                else
                                    showProvideDetails(stepNum + 1, numSteps, (String) proxyCode);
                            }
                        });
                    }
                });
            }
        });
    }

    private void showGauthDetails(final int stepNum, final int numSteps, final String proxyCode) {

        setView(R.layout.activity_two_factor_3_gauth_details);
        final ImageView imageView = UI.find(this, R.id.imageView);
        final TextView textCode = UI.find(this, R.id.textCode);

        mProgressBar.setProgress(stepNum);
        mProgressBar.setMax(numSteps);

        final String gauth_url = (String) mService.getTwoFactorConfig().get("gauth_url");
        final BitmapDrawable bd = new BitmapDrawable(getResources(), new QrBitmap(gauth_url, 0).getQRCode());
        bd.setFilterBitmap(false);
        imageView.setImageDrawable(bd);

        final String gauthCode = gauth_url.split("=")[1];
        textCode.setText(getString(R.string.twoFacGauthTextCode, gauthCode));

        textCode.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        final ClipboardManager clipboard = (ClipboardManager)
                                getSystemService(Context.CLIPBOARD_SERVICE);
                        final ClipData clip = ClipData.newPlainText("data", gauthCode);
                        clipboard.setPrimaryClip(clip);
                        TwoFactorActivity.this.toast(R.string.warnOnPaste);
                    }
                }
        );

        mContinueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final Map<String, String> twoFacData = make2FAData("proxy", proxyCode);
                mContinueButton.setEnabled(false);
                CB.after(mService.enableTwoFactor("gauth", UI.getText(mCodeText).trim(), twoFacData),
                         new CB.Toast<Boolean>(TwoFactorActivity.this, mContinueButton) {
                    @Override
                    public void onSuccess(final Boolean result) {
                        setResult(RESULT_OK);
                        finishOnUiThread();
                    }
                });
            }
        });
    }

    private void showProvideConfirmationCode(final int stepNum, final int numSteps) {

        setView(R.layout.activity_two_factor_2_4_provide_code);
        mPromptText.setText(getTypeString(UI.getText(mPromptText), mLocalizedMap.get(mMethod)));

        mProgressBar.setProgress(stepNum);
        mProgressBar.setMax(numSteps);

        mContinueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final String enteredCode = UI.getText(mCodeText).trim();
                if (enteredCode.length() != 6)
                    return;
                mContinueButton.setEnabled(false);
                CB.after(mService.enableTwoFactor(mMethod, enteredCode, null),
                         new CB.Toast<Boolean>(TwoFactorActivity.this, mContinueButton) {
                    @Override
                    public void onSuccess(final Boolean result) {
                        setResult(RESULT_OK);
                        finishOnUiThread();
                    }
                });
            }
        });
    }
}
