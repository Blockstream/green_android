package com.greenaddress.greenbits.ui.preferences;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.greenaddress.greenbits.ui.GaActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.TwoFactorActivity;

import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TwoFactorPreferenceFragment extends GAPreferenceFragment {

    private static final int REQUEST_ENABLE_2FA = 0;
    private String twoFacMethod;

    private final CheckBoxPreference getPref(final String method) {
        return (CheckBoxPreference) findPreference("twoFac" + method);
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

        final Map<?, ?> config = gaService.getTwoFacConfig();
        if (config == null || config.isEmpty()) {
            Toast.makeText(getActivity(), getString(R.string.err_send_not_connected_will_resume), Toast.LENGTH_LONG).show();
            getActivity().finish();
        }
        setupCheckbox(config, "Email");
        setupCheckbox(config, "Gauth");
        setupCheckbox(config, "SMS");
        setupCheckbox(config, "Phone");
    }

    private void change2FA(@NonNull final String method, final Boolean newValue) {
        if (newValue) {
            final Intent intent = new Intent(this.getActivity(), TwoFactorActivity.class);
            intent.putExtra("method", method.toLowerCase());
            twoFacMethod = method;
            startActivityForResult(intent, REQUEST_ENABLE_2FA);
            return;
        }

        final String[] enabledTwoFacNames = new String[]{};
        final List<String> enabledTwoFacNamesSystem = gaService.getEnabledTwoFacNames(true);
        if (enabledTwoFacNamesSystem.size() <= 1) {
            disable2FA(method, enabledTwoFacNamesSystem.get(0));
            return;
        }
        GaActivity.Popup(this.getActivity(), getString(R.string.twoFactorChoicesTitle),
                         R.string.choose, R.string.cancel)
                  .items(gaService.getEnabledTwoFacNames(false).toArray(enabledTwoFacNames))
                  .itemsCallbackSingleChoice(0, new MaterialDialog.ListCallbackSingleChoice() {
                      @Override
                      public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                          disable2FA(method, enabledTwoFacNamesSystem.get(which));
                          return true;
                      }
                  }).build().show();
    }

    private void disable2FA(@NonNull final String method, @NonNull final String withMethod) {
        if (!withMethod.equals("gauth")) {
            final Map<String, String> data = new HashMap<>();
            data.put("method", method.toLowerCase());
            gaService.requestTwoFacCode(withMethod, "disable_2fa", data);
        }
        final View inflatedLayout = getActivity().getLayoutInflater().inflate(R.layout.dialog_btchip_pin, null, false);
        final EditText twoFacValue = (EditText) inflatedLayout.findViewById(R.id.btchipPINValue);
        final TextView prompt = (TextView) inflatedLayout.findViewById(R.id.btchipPinPrompt);
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
        prompt.setText(new Formatter().format(
                getResources().getString(R.string.twoFacProvideConfirmationCode),
                withMethodName).toString());

        GaActivity.Popup(this.getActivity(), "2FA")
                  .customView(inflatedLayout, true)
                  .onPositive(new MaterialDialog.SingleButtonCallback() {
                      @Override
                      public void onClick(final @NonNull MaterialDialog dialog, final @NonNull DialogAction which) {
                          final Map<String, String> twoFacData = new HashMap<>();
                          twoFacData.put("method", withMethod);
                          twoFacData.put("code", twoFacValue.getText().toString());
                          Futures.addCallback(gaService.disableTwoFac(method.toLowerCase(), twoFacData), new FutureCallback<Boolean>() {
                              @Override
                              public void onSuccess(final @Nullable Boolean result) {
                                  final CheckBoxPreference c = (CheckBoxPreference) getPreferenceManager().findPreference("twoFac" + method);
                                  c.setChecked(false);
                              }

                              @Override
                              public void onFailure(@NonNull final Throwable t) {
                                  t.printStackTrace();
                                  Toast.makeText(TwoFactorPreferenceFragment.this.getActivity(), t.getMessage(), Toast.LENGTH_LONG).show();
                              }
                          });
                      }
                  }).build().show();
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (resultCode == Activity.RESULT_OK)
            getPref(twoFacMethod).setChecked(true);
        else
            super.onActivityResult(requestCode, resultCode, data);
    }
}
