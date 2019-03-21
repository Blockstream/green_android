package com.greenaddress.greenbits.ui.preferences;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.Preference;

import com.greenaddress.greenbits.ui.BuildConfig;
import com.greenaddress.greenbits.ui.LoggedActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;

public class WatchOnlyPreferenceFragment extends GAPreferenceFragment
    implements Preference.OnPreferenceClickListener {
    private static final String TAG = WatchOnlyPreferenceFragment.class.getSimpleName();

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);

        addPreferencesFromResource(R.xml.preference_watchonly);
        setHasOptionsMenu(true);

        // Network & Logout
        final Preference logout = find(PrefKeys.LOGOUT);
        logout.setTitle(getString(R.string.id_s_network, mService.getNetwork().getName()));
        logout.setSummary(UI.getColoredString(
                              getString(R.string.id_log_out), ContextCompat.getColor(getContext(), R.color.red)));
        logout.setOnPreferenceClickListener(preference -> {
            logout.setEnabled(false);
            ((LoggedActivity) getActivity()).logout();
            return false;
        });

        // Version
        final Preference version = find("version");
        version.setSummary(String.format("%s %s",
                                         getString(R.string.app_name),
                                         getString(R.string.id_version_1s_2s,
                                                   BuildConfig.VERSION_NAME,
                                                   BuildConfig.BUILD_TYPE)));

        ((Preference) find("logout")).setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(final Preference preference) {
        switch (preference.getKey()) {
        case "logout":
            ((LoggedActivity) getActivity()).logout();
            return true;
        }
        return false;
    }



}
