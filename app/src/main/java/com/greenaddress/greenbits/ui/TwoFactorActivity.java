package com.greenaddress.greenbits.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.QrBitmap;

import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TwoFactorActivity extends GaActivity {

    private String twoFacType, twoFacTypeName;

    private View inflateView(final int id) {
        final View v = getLayoutInflater().inflate(id, null, false);
        v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_right));
        setContentView(v);
        return v;
    }

    @Override
    protected void onCreateWithService(Bundle savedInstanceState) {

        final GaService service = mService;

        if (service.getTwoFacConfig() == null) {
            finish();
            return;
        }

        twoFacType = getIntent().getStringExtra("method");
        final String[] allTwoFac = getResources().getStringArray(R.array.twoFactorChoices);
        final String[] allTwoFacSystem = getResources().getStringArray(R.array.twoFactorChoicesSystem);
        final List<String> enabledTwoFacNames = service.getEnabledTwoFacNames(false);
        final List<String> enabledTwoFacNamesSystem = service.getEnabledTwoFacNames(true);
        for (int i = 0; i < allTwoFacSystem.length; ++i) {
            if (allTwoFacSystem[i].equals(twoFacType)) {
                twoFacTypeName = allTwoFac[i];
                break;
            }
        }
        setTitle(new Formatter().format(getTitle().toString(), twoFacTypeName).toString());

        if (enabledTwoFacNames.size() > 1) {
            setContentView(R.layout.activity_two_factor_1_choose);
            final Button continueButton = UI.find(this, R.id.continueButton);
            final RadioGroup radioGroup = UI.find(this, R.id.radioGroup);
            radioGroup.removeViews(0, radioGroup.getChildCount());

            for (int i = 0; i < enabledTwoFacNames.size(); ++i) {
                RadioButton button = new RadioButton(TwoFactorActivity.this);
                button.setText(enabledTwoFacNames.get(i));
                button.setId(i);
                radioGroup.addView(button);
            }

            radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(final RadioGroup group, final int checkedId) {
                    continueButton.setEnabled(true);
                }
            });

            continueButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final String methodName = enabledTwoFacNamesSystem.get(radioGroup.getCheckedRadioButtonId());
                    final int stepsCount;
                    if (twoFacType.equals("gauth")) {
                        // details and confirmation code together
                        stepsCount = 3;
                    } else {
                        stepsCount = 4;
                    }
                    if (!methodName.equals("gauth")) {
                        final Map<String, String> data = new HashMap<>();
                        data.put("method", twoFacType);
                        service.requestTwoFacCode(methodName, "enable_2fa", data);
                    }
                    showProvideAuthCode(2, stepsCount, enabledTwoFacNames.get(radioGroup.getCheckedRadioButtonId()),
                            methodName, twoFacType);
                }
            });
        } else if (enabledTwoFacNames.size() == 1) {
            // just one 2FA enabled - go straight to code verification
            final String methodName = enabledTwoFacNamesSystem.get(0);
            final int stepsCount;
            if (twoFacType.equals("gauth")) {
                // details and confirmation code together
                stepsCount = 2;
            } else {
                stepsCount = 3;
            }
            if (!methodName.equals("gauth")) {
                final Map<String, String> data = new HashMap<>();
                data.put("method", twoFacType);
                service.requestTwoFacCode(methodName, "enable_2fa", data);
            }
            showProvideAuthCode(1, stepsCount, enabledTwoFacNames.get(0),
                    methodName, twoFacType);
        } else {
            // no 2FA enabled - go straight to 2FA details
            if (twoFacType.equals("gauth")) {
                showGauthDetails(1, 1, null);
            } else {
                showProvideDetails(1, 2, null);
            }
        }
    }

    private void showProvideDetails(final int stepNum, final int numSteps, final String proxyCode) {
        final GaService service = mService;
        setContentView(R.layout.activity_two_factor_3_provide_details);
        final Button continueButton = UI.find(this, R.id.continueButton);
        final TextView prompt = UI.find(this, R.id.prompt);
        final TextView details = UI.find(this, R.id.details);
        prompt.setText(new Formatter().format(
                UI.getText(prompt),
                twoFacType.equals("email") ?
                        getResources().getString(R.string.emailAddress) :
                        getResources().getString(R.string.phoneNumber)).toString());
        if (!twoFacType.equals("email"))
            UI.hide((View) UI.find(this, R.id.emailNotices));

        final ProgressBar progressBar = UI.find(this, R.id.progressBar);
        progressBar.setProgress(stepNum);
        progressBar.setMax(numSteps);

        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (UI.getText(details).trim().isEmpty()) return;
                continueButton.setEnabled(false);
                final Map<String, String> twoFacData = new HashMap<>();
                if (proxyCode != null) {  // two factor required
                    twoFacData.put("method", "proxy");
                    twoFacData.put("code", proxyCode);
                }
                CB.after(service.initEnableTwoFac(twoFacType, UI.getText(details), twoFacData),
                         new CB.Toast<Boolean>(TwoFactorActivity.this, continueButton) {
                    @Override
                    public void onSuccess(final Boolean result) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showProvideConfirmationCode(stepNum + 1, numSteps);
                            }
                        });
                    }
                });
            }
        });
    }

    private void showProvideAuthCode(final int stepNum, final int numSteps, final String oldMethodName, final String oldMethod, final String newMethod) {
        final GaService service = mService;
        inflateView(R.layout.activity_two_factor_2_4_provide_code);

        final TextView description = UI.find(this, R.id.description);
        final TextView prompt = UI.find(this, R.id.prompt);
        final EditText code = UI.find(this, R.id.code);
        description.setText(R.string.twoFacProvideAuthCodeDescription);
        prompt.setText(new Formatter().format(UI.getText(prompt), oldMethodName).toString());
        final ProgressBar progressBar = UI.find(this, R.id.progressBar);
        progressBar.setProgress(stepNum);
        progressBar.setMax(numSteps);

        final Button continueButton = UI.find(this, R.id.continueButton);
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                continueButton.setEnabled(false);
                final Map<String, String> data = new HashMap<>();
                data.put("method", oldMethod);
                data.put("code", UI.getText(code));
                CB.after(service.requestTwoFacCode("proxy", newMethod, data),
                         new CB.Toast<Object>(TwoFactorActivity.this, continueButton) {
                    @Override
                    public void onSuccess(final Object proxyCode) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (newMethod.equals("gauth")) {
                                    showGauthDetails(stepNum + 1, numSteps, (String) proxyCode);
                                } else {
                                    showProvideDetails(stepNum + 1, numSteps, (String) proxyCode);
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    private void showGauthDetails(final int stepNum, final int numSteps, final String proxyCode) {
        final GaService service = mService;

        inflateView(R.layout.activity_two_factor_3_gauth_details);
        final ImageView imageView = UI.find(this, R.id.imageView);
        final TextView textCode = UI.find(this, R.id.textCode);
        final Button continueButton = UI.find(this, R.id.continueButton);
        final EditText code = UI.find(this, R.id.code);
        final ProgressBar progressBar = UI.find(this, R.id.progressBar);
        progressBar.setProgress(stepNum);
        progressBar.setMax(numSteps);

        final String gauth_url = (String) service.getTwoFacConfig().get("gauth_url");
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

        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Map<String, String> twoFacData = new HashMap<>();
                if (proxyCode != null) {
                    twoFacData.put("method", "proxy");
                    twoFacData.put("code", proxyCode);
                }
                continueButton.setEnabled(false);
                CB.after(service.enableTwoFactor("gauth", UI.getText(code).trim(), twoFacData),
                         new CB.Toast<Boolean>(TwoFactorActivity.this, continueButton) {
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
        final GaService service = mService;

        inflateView(R.layout.activity_two_factor_2_4_provide_code);
        final Button continueButton = UI.find(this, R.id.continueButton);
        final EditText code = UI.find(this, R.id.code);
        final TextView prompt = UI.find(this, R.id.prompt);
        prompt.setText(new Formatter().format(UI.getText(prompt), twoFacTypeName).toString());
        final ProgressBar progressBar = UI.find(this, R.id.progressBar);
        progressBar.setProgress(stepNum);
        progressBar.setMax(numSteps);

        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String enteredCode = UI.getText(code).trim();
                if (enteredCode.length() != 6)
                    return;
                continueButton.setEnabled(false);
                CB.after(service.enableTwoFactor(twoFacType, enteredCode, null),
                         new CB.Toast<Boolean>(TwoFactorActivity.this, continueButton) {
                    @Override
                    public void onSuccess(Boolean result) {
                        setResult(RESULT_OK);
                        finishOnUiThread();
                    }
                });
            }
        });
    }
}
