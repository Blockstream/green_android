package com.greenaddress.greenbits.ui.preferences;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;

import com.greenaddress.greenbits.ui.BuildConfig;
import com.greenaddress.greenbits.ui.LoggedActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.TwoFactorActivity;

import static android.app.Activity.RESULT_OK;

public class ResetActivePreferenceFragment extends GAPreferenceFragment
    implements Preference.OnPreferenceClickListener {
    private static final String TAG = ResetActivePreferenceFragment.class.getSimpleName();
    private static final int REQUEST_2FA = 101;

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);

        addPreferencesFromResource(R.xml.preference_resetactive);
        setHasOptionsMenu(true);

        // Network & Logout
        final PreferenceCategory cat = find(PrefKeys.NETWORK_CATEGORY);
        cat.setTitle(getString(R.string.id_s_network, mService.getNetwork().getName().toUpperCase()));
        final Preference logout = find(PrefKeys.LOGOUT);
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

        // Actions
        ((Preference) find("logout")).setOnPreferenceClickListener(this);
        ((Preference) find("cancel_twofactor_reset")).setOnPreferenceClickListener(this);
        ((Preference) find("dispute_twofactor_reset")).setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(final Preference preference) {
        switch (preference.getKey()) {
        case "logout":
            ((LoggedActivity) getActivity()).logout();
            return true;
        case "cancel_twofactor_reset":
            startTwoFactorActivity("cancel");
            return true;
        case "dispute_twofactor_reset":
            startTwoFactorActivity("dispute");
            return true;
        }
        return false;
    }

    private void startTwoFactorActivity(final String method) {
        final Intent intent = new Intent(getActivity(), TwoFactorActivity.class);
        intent.putExtra("method", method);
        startActivityForResult(intent, REQUEST_2FA);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // If reset cancelled or disputed with success, logout
        final int unmaskedRequestCode = requestCode & 0x0000ffff;
        if (unmaskedRequestCode == 101 && resultCode == RESULT_OK)
            ((LoggedActivity) getActivity()).logout();
    }
}
