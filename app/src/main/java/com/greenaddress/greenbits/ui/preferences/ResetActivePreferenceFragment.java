package com.greenaddress.greenbits.ui.preferences;

import android.content.Intent;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.preference.Preference;

import com.greenaddress.greenbits.ui.BuildConfig;
import com.greenaddress.greenbits.ui.LoggedActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;
import com.greenaddress.greenbits.ui.twofactor.TwoFactorActivity;

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

        if (getGAApp().getModel() == null) {
            logout();
            return;
        }

        //  Logout
        final Preference logout = find(PrefKeys.LOGOUT);
        logout.setTitle(getString(R.string.id_s_network, getNetwork().getName()));
        logout.setSummary(UI.getColoredString(
                              getString(R.string.id_log_out), ContextCompat.getColor(getContext(), R.color.red)));
        logout.setOnPreferenceClickListener(preference -> {
            logout.setEnabled(false);
            logout();
            return false;
        });

        // Version
        final Preference version = find(PrefKeys.VERSION);
        version.setSummary(String.format("%s %s",
                                         getString(R.string.app_name),
                                         getString(R.string.id_version_1s_2s,
                                                   BuildConfig.VERSION_NAME,
                                                   BuildConfig.BUILD_TYPE)));

        // Terms of service
        final Preference termsOfUse = find(PrefKeys.TERMS_OF_USE);
        termsOfUse.setOnPreferenceClickListener(preference -> openURI("https://blockstream.com/green/terms/"));

        // Privacy policy
        final Preference privacyPolicy = find(PrefKeys.PRIVACY_POLICY);
        privacyPolicy.setOnPreferenceClickListener(preference -> openURI("https://blockstream.com/green/privacy/"));

        // Actions
        ((Preference) find(PrefKeys.CANCEL_TWOFACTOR_RESET)).setOnPreferenceClickListener(this);
        ((Preference) find(PrefKeys.DISPUTE_TWOFACTOR_RESET)).setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(final Preference preference) {
        switch (preference.getKey()) {
        case PrefKeys.CANCEL_TWOFACTOR_RESET:
            startTwoFactorActivity("cancel");
            return true;
        case PrefKeys.DISPUTE_TWOFACTOR_RESET:
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
