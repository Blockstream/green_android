package com.greenaddress.greenbits.ui.preferences;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.ui.CB;
import com.greenaddress.greenbits.ui.UI;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.TwoFactorActivity;

import java.util.concurrent.Callable;
import java.util.HashMap;
import java.util.Map;

public class TwoFactorPreferenceFragment extends GAPreferenceFragment {

    private static final int REQUEST_ENABLE_2FA = 0;
    private static final String NLOCKTIME_EMAILS = "NLocktimeEmails";

    private String mMethod; // Current 2FA Method
    private Map<String, String> mLocalizedMap; // 2FA method to localized description
    private EditTextPreference mLimitsPref;

    private CheckBoxPreference getPref(final String method) {
        return find("twoFac" + method);
    }

    private boolean isEnabled(final Map<?, ?> twoFacConfig, final String method) {
        return twoFacConfig.get(method.toLowerCase()).equals(true);
    }

    private boolean haveAny2FA(final Map<?, ?> twoFacConfig) {
        return twoFacConfig != null && (Boolean) twoFacConfig.get("any");
    }

    private CheckBoxPreference setupCheckbox(final Map<?, ?> twoFacConfig, final String method) {
        final CheckBoxPreference c = getPref(method);
        if (method.equals(NLOCKTIME_EMAILS))
            c.setChecked(isNlocktimeConfig(true));
        else
            c.setChecked(isEnabled(twoFacConfig, method));
        c.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference p, final Object newValue) {
                if (method.equals(NLOCKTIME_EMAILS))
                    setNlocktimeConfig((Boolean) newValue);
                else
                    prompt2FAChange(method, (Boolean) newValue);
                return false;
            }
        });
        return c;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLocalizedMap = UI.getTwoFactorLookup(getResources());

        addPreferencesFromResource(R.xml.preference_twofactor);
        setHasOptionsMenu(true);

        final Map<?, ?> twoFacConfig = mService.getTwoFactorConfig();
        if (twoFacConfig == null || twoFacConfig.isEmpty()) {
            final GaPreferenceActivity activity = (GaPreferenceActivity) getActivity();
            activity.toast(R.string.err_send_not_connected_will_resume);
            activity.finish();
            return;
        }
        final CheckBoxPreference emailCB = setupCheckbox(twoFacConfig, "Email");
        setupCheckbox(twoFacConfig, "Gauth");
        setupCheckbox(twoFacConfig, "SMS");
        setupCheckbox(twoFacConfig, "Phone");

        mLimitsPref = find("twoFacLimits");
        if (!GaService.IS_ELEMENTS) {
            // Value -> satoshi conversion needs implementation & testing for
            // non-Elements (currently it's simply float(str) * 100)
            removePreference(mLimitsPref);
        } else {
            mLimitsPref.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            mLimitsPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                    return onLimitsPreferenceChange(preference, newValue);
                }
            });
            // Can only set limits if at least one 2FA method is available
            final boolean haveAny = haveAny2FA(twoFacConfig);
            mLimitsPref.setEnabled(haveAny);
            mLimitsPref.setSummary(haveAny ? R.string.twoFacLimitEnabled : R.string.twoFacLimitDisabled);
        }

        if (GaService.IS_ELEMENTS)
            removePreference(getPref(NLOCKTIME_EMAILS));
        else {
            final CheckBoxPreference nlockCB = setupCheckbox(twoFacConfig, NLOCKTIME_EMAILS);
            nlockCB.setEnabled(emailCB.isChecked());
        }
    }

    private boolean isNlocktimeConfig(final Boolean enabled) {
        Boolean b = false;
        final Map<String, Object> outer;
        outer = (Map) mService.getUserConfig("notifications_settings");
        if (outer != null)
            b = Boolean.TRUE.equals(outer.get("email_incoming")) &&
                Boolean.TRUE.equals(outer.get("email_outgoing"));
        return b.equals(enabled);
    }

    private void setNlocktimeConfig(final Boolean enabled) {
        if (GaService.IS_ELEMENTS || isNlocktimeConfig(enabled))
            return; // Nothing to do
        final Map<String, Object> inner, outer;
        inner = ImmutableMap.of("email_incoming", (Object) enabled,
                                "email_outgoing", (Object) enabled);
        outer = ImmutableMap.of("notifications_settings", (Object) inner);
        mService.setUserConfig(Maps.newHashMap(outer), true /* Immediate */);
        getPref(NLOCKTIME_EMAILS).setChecked(enabled);
    }

    private void prompt2FAChange(final String method, final Boolean newValue) {
        if (newValue) {
            final Intent intent = new Intent(getActivity(), TwoFactorActivity.class);
            intent.putExtra("method", method.toLowerCase());
            mMethod = method;
            startActivityForResult(intent, REQUEST_ENABLE_2FA);
            return;
        }

        final boolean skipChoice = false;
        final Dialog dlg = UI.popupTwoFactorChoice(getActivity(), mService, skipChoice,
                                                   new CB.Runnable1T<String>() {
            @Override
            public void run(final String withMethod) {
                disable2FA(method, withMethod);
            }
        });
        if (dlg != null)
            dlg.show();
    }

    private void setLimit(final Long newValue, final Map<String, String> twoFacData) {
        try {
            mService.changeTxLimits(newValue, twoFacData);
        } catch (final Exception e) {
            e.printStackTrace();
            UI.toast(getActivity(), "Failed", Toast.LENGTH_SHORT);
            return;
        }
        mLimitsPref.setText(String.valueOf(((float) newValue) / 100));
    }

    private void disable2FA(final String method, final String withMethod) {
        if (!withMethod.equals("gauth")) {
            final Map<String, String> data = new HashMap<>();
            data.put("method", method.toLowerCase());
            mService.requestTwoFacCode(withMethod, "disable_2fa", data);
        }

        final View v = inflatePinDialog(withMethod);
        final EditText codeText = UI.find(v, R.id.btchipPINValue);

        UI.popup(this.getActivity(), "2FA")
                  .customView(v, true)
                  .onPositive(new MaterialDialog.SingleButtonCallback() {
                      @Override
                      public void onClick(final MaterialDialog dialog, final DialogAction which) {
                          mService.getExecutor().submit(new Callable<Void>() {
                              @Override
                              public Void call() {
                                  disable2FAImpl(method, withMethod, UI.getText(codeText));
                                  return null;
                              }
                          });
                      }
                  }).build().show();
    }

    private void disable2FAImpl(final String method, final String withMethod, final String code) {
        try {
            final Map<String, String> twoFacData = new HashMap<>();
            twoFacData.put("method", withMethod);
            twoFacData.put("code", code);
            if (mService.disableTwoFactor(method.toLowerCase(), twoFacData)) {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        change2FA(method, false);
                    }
                });
                return;
            }
        } catch (final Exception e) {
            // Toast below
        }
        UI.toast(getActivity(), "Error disabling 2FA", Toast.LENGTH_SHORT);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (resultCode == Activity.RESULT_OK)
            change2FA(mMethod, true);
        else
            super.onActivityResult(requestCode, resultCode, data);
    }

    private void change2FA(final String method, final Boolean checked) {
        getPref(method).setChecked(checked);
        if (method.equals("Email")) {
            // Reset nlocktime prefs when the user changes email 2FA
            setNlocktimeConfig(checked);
            if (!GaService.IS_ELEMENTS)
                getPref(NLOCKTIME_EMAILS).setEnabled(checked);
        }
        final boolean haveAny;
        if (checked) {
            // Simple case when enabling a new 2FA method
            haveAny = true;
        } else {
            // If disabled, we know the services cached 2FA info is current
            haveAny = haveAny2FA(mService.getTwoFactorConfig());
        }
        mLimitsPref.setEnabled(haveAny);
        mLimitsPref.setSummary(haveAny ? R.string.twoFacLimitEnabled : R.string.twoFacLimitDisabled);
    }

    private View inflatePinDialog(final String withMethod) {
        final View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_btchip_pin, null, false);

        final TextView promptText = UI.find(v, R.id.btchipPinPrompt);
        promptText.setText(getString(R.string.twoFacProvideConfirmationCode,
                                     mLocalizedMap.get(withMethod)));
        return v;
    }

    private boolean onLimitsPreferenceChange(final Preference preference, final Object newValue) {
        final Float limitF = Float.valueOf((String) newValue);
        final long limit = (long) (limitF.floatValue() * 100);

        final String existingLimit = mLimitsPref.getText();
        if (!TextUtils.isEmpty(existingLimit) && limitF <= Float.valueOf(existingLimit)) {
            setLimit(limit, null); // Don't require 2FA to lower the limit
            return true;
        }

        final boolean skipChoice = false;
        final Dialog dlg = UI.popupTwoFactorChoice(getActivity(), mService, skipChoice,
            new CB.Runnable1T<String>() {
                @Override
                public void run(final String withMethod) {
                    final View v = inflatePinDialog(withMethod);
                    final EditText codeText = UI.find(v, R.id.btchipPINValue);

                    if (!withMethod.equals("gauth")) {
                        final Map<String, Object> data = new HashMap<>();
                        data.put("is_fiat", false);
                        data.put("per_tx", 0);
                        data.put("total", limit);

                        mService.requestTwoFacCode(withMethod, "change_tx_limits", data);
                    }

                    UI.popup(getActivity(), "2FA")
                            .customView(v, true)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(final MaterialDialog dialog, final DialogAction which) {
                                    final Map<String, String> twoFacData = new HashMap<>();
                                    twoFacData.put("method", withMethod);
                                    twoFacData.put("code", UI.getText(codeText));
                                    setLimit(limit, twoFacData);
                                }
                            }).build().show();
                }
            });

        if (dlg != null)
            dlg.show();
        return false;
    }
}
