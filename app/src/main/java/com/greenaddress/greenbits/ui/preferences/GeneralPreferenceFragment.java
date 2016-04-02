package com.greenaddress.greenbits.ui.preferences;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.support.annotation.NonNull;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.greenaddress.greenbits.ui.BuildConfig;
import com.greenaddress.greenbits.ui.R;

import javax.annotation.Nullable;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class GeneralPreferenceFragment extends GAPreferenceFragment {
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference_general);
        setHasOptionsMenu(true);

        // -- handle timeout
        final SharedPreferences.Editor editor = getPreferenceManager().getSharedPreferences().edit();
        int timeout = 5;
        try {
            timeout = (int) gaService.getAppearanceValue("altimeout");
        } catch (@NonNull final Exception e) {
            // not set
        }
        editor.putString("altime", Integer.toString(timeout));
        editor.apply();
        final Preference altime = findPreference("altime");
        altime.setSummary(String.format("%d %s", timeout, getResources().getString(R.string.autologout_time_default)));
        altime.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                try {
                    final Integer altimeout = Integer.parseInt(newValue.toString());
                    gaService.setAppearanceValue("altimeout", altimeout, true);
                    preference.setSummary(String.format("%d %s", altimeout, getResources().getString(R.string.autologout_time_default)));
                    return true;
                } catch (@NonNull final Exception e) {
                    // not set
                }
                return false;
            }
        });

        // -- handle mnemonics

        final String mnemonic = gaService.getMnemonics();
        if (mnemonic != null) {
            findPreference("mnemonic_passphrase").setSummary(getString(R.string.touch_to_display));
            findPreference("mnemonic_passphrase").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(final Preference preference) {
                    getPreferenceManager().findPreference("mnemonic_passphrase").setSummary(mnemonic);
                    return false;
                }
            });
        }

        // -- handle version

        findPreference("app_version").setSummary(String.format(
                "%s, %s",
                BuildConfig.VERSION_NAME,
                BuildConfig.BUILD_TYPE));


        // -- handle opt-in rbf
        if (!gaService.getClient().getLoginData().rbf) {
            getPreferenceScreen().removePreference(findPreference("optin_rbf"));
        } else {
            final CheckBoxPreference optin_rbf = (CheckBoxPreference) findPreference("optin_rbf");
            optin_rbf.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                    Futures.addCallback(
                            gaService.setAppearanceValue("replace_by_fee", newValue, false),
                            new FutureCallback<Boolean>() {
                                @Override
                                public void onSuccess(final @Nullable Boolean result) {
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            optin_rbf.setChecked((Boolean) newValue);
                                            optin_rbf.setEnabled(true);
                                        }
                                    });
                                }

                                @Override
                                public void onFailure(final Throwable t) {
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            optin_rbf.setEnabled(true);
                                        }
                                    });
                                }
                            });
                    // disable until server confirms set
                    optin_rbf.setEnabled(false);
                    return false;
                }
            });
            final Boolean replace_by_fee = (Boolean) gaService.getAppearanceValue("replace_by_fee");
            ((CheckBoxPreference) findPreference("optin_rbf")).setChecked(replace_by_fee);
        }
    }
}