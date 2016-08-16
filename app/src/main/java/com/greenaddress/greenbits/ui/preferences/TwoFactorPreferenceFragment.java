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
    private String mTwoFactorMethod;

    private CheckBoxPreference getPref(final String method) {
        return find("twoFac" + method);
    }

    private void setupCheckbox(final Map<?, ?> config, final String method) {
        final CheckBoxPreference c = getPref(method);
        c.setChecked(config.get(method.toLowerCase()).equals(true));
        c.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference p, final Object newValue) {
                change2FA(method, (Boolean) newValue);
                return false;
            }
        });
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
        setupCheckbox(config, "Email");
        setupCheckbox(config, "Gauth");
        setupCheckbox(config, "SMS");
        setupCheckbox(config, "Phone");
    }

    private void change2FA(final String method, final Boolean newValue) {
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
            public void run(final String whichMethod) {
                disable2FA(method, whichMethod);
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
        final EditText twoFacValue = UI.find(v, R.id.btchipPINValue);
        final TextView prompt = UI.find(v, R.id.btchipPinPrompt);
        final String[] allTwoFac = getResources().getStringArray(R.array.twoFactorChoices);
        final String[] allTwoFacSystem = getResources().getStringArray(R.array.twoFactorChoicesSystem);
        String withMethodName = "";
        int i = 0;
        for (final String name : allTwoFacSystem) {
            if (name.equals(withMethod)) {
                withMethodName = allTwoFac[i];
                break;
            }
            i++;
        }
        prompt.setText(getString(R.string.twoFacProvideConfirmationCode, withMethodName));

        UI.popup(this.getActivity(), "2FA")
                  .customView(v, true)
                  .onPositive(new MaterialDialog.SingleButtonCallback() {
                      @Override
                      public void onClick(final MaterialDialog dialog, final DialogAction which) {
                          final Map<String, String> twoFacData = new HashMap<>();
                          twoFacData.put("method", withMethod);
                          twoFacData.put("code", UI.getText(twoFacValue));
                          Futures.addCallback(mService.disableTwoFac(method.toLowerCase(), twoFacData), new FutureCallback<Boolean>() {
                              @Override
                              public void onSuccess(final Boolean result) {
                                  getActivity().runOnUiThread(new Runnable() {
                                      @Override
                                      public void run() {
                                          getPref(method).setChecked(false);
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
            getPref(mTwoFactorMethod).setChecked(true);
        else
            super.onActivityResult(requestCode, resultCode, data);
    }
}
