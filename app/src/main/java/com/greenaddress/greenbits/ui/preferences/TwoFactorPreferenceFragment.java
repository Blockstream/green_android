package com.greenaddress.greenbits.ui.preferences;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.greenaddress.greenbits.ui.CB;
import com.greenaddress.greenbits.ui.UI;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.TwoFactorActivity;

import java.util.HashMap;
import java.util.Map;

public class TwoFactorPreferenceFragment extends GAPreferenceFragment {

    private static final int REQUEST_ENABLE_2FA = 0;
    private static final String NLOCKTIME_EMAILS = "NLocktimeEmails";
    private String mTwoFactorMethod;

    private CheckBoxPreference getPref(final String method) {
        return find("twoFac" + method);
    }

    private CheckBoxPreference setupCheckbox(final Map<?, ?> config, final String method) {
        final CheckBoxPreference c = getPref(method);
        if (method.equals(NLOCKTIME_EMAILS))
            c.setChecked(isNlocktimeConfig(true));
        else
            c.setChecked(config.get(method.toLowerCase()).equals(true));
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
        addPreferencesFromResource(R.xml.preference_twofactor);
        setHasOptionsMenu(true);

        final Map<?, ?> config = mService.getTwoFactorConfig();
        if (config == null || config.isEmpty()) {
            final GaPreferenceActivity activity = (GaPreferenceActivity) getActivity();
            activity.toast(R.string.err_send_not_connected_will_resume);
            activity.finish();
            return;
        }
        final CheckBoxPreference emailCB = setupCheckbox(config, "Email");
        setupCheckbox(config, "Gauth");
        setupCheckbox(config, "SMS");
        setupCheckbox(config, "Phone");
        final CheckBoxPreference nlockCB = setupCheckbox(config, NLOCKTIME_EMAILS);
        nlockCB.setEnabled(emailCB.isChecked());
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
        if (isNlocktimeConfig(enabled))
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
            mTwoFactorMethod = method;
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

    private void disable2FA(final String method, final String withMethod) {
        if (!withMethod.equals("gauth")) {
            final Map<String, String> data = new HashMap<>();
            data.put("method", method.toLowerCase());
            mService.requestTwoFacCode(withMethod, "disable_2fa", data);
        }
        final View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_btchip_pin, null, false);

        final Map<String, String> localizedMap = UI.getTwoFactorLookup(getResources());

        final TextView promptText = UI.find(v, R.id.btchipPinPrompt);
        promptText.setText(getString(R.string.twoFacProvideConfirmationCode,
                                     localizedMap.get(withMethod)));

        final EditText codeText = UI.find(v, R.id.btchipPINValue);
        UI.popup(this.getActivity(), "2FA")
                  .customView(v, true)
                  .onPositive(new MaterialDialog.SingleButtonCallback() {
                      @Override
                      public void onClick(final MaterialDialog dialog, final DialogAction which) {
                          final Map<String, String> twoFacData = new HashMap<>();
                          twoFacData.put("method", withMethod);
                          twoFacData.put("code", UI.getText(codeText));
                          Futures.addCallback(mService.disableTwoFac(method.toLowerCase(), twoFacData), new FutureCallback<Boolean>() {
                              @Override
                              public void onSuccess(final Boolean result) {
                                  getActivity().runOnUiThread(new Runnable() {
                                      public void run() {
                                          change2FA(method, false);
                                      }
                                  });
                              }

                              @Override
                              public void onFailure(final Throwable t) {
                                  t.printStackTrace();
                                  ((GaPreferenceActivity) getActivity()).toast(t.getMessage());
                              }
                          });
                      }
                  }).build().show();
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (resultCode == Activity.RESULT_OK)
            change2FA(mTwoFactorMethod, true);
        else
            super.onActivityResult(requestCode, resultCode, data);
    }

    private void change2FA(final String method, final Boolean checked) {
        getPref(method).setChecked(checked);
        if (method.equals("Email")) {
            // Reset nlocktime prefs when the user changes email 2FA
            setNlocktimeConfig(checked);
            getPref(NLOCKTIME_EMAILS).setEnabled(checked);
        }
    }
}
