package com.greenaddress.greenbits.ui.preferences;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.support.annotation.NonNull;

import com.greenaddress.greenbits.ui.R;

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
    }
}