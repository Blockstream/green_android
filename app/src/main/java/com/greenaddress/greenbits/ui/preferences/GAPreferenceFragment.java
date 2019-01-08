package com.greenaddress.greenbits.ui.preferences;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.MenuItem;
import android.widget.Toast;

import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.GreenAddressApplication;
import com.greenaddress.greenbits.ui.LoggedActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;

public class GAPreferenceFragment extends PreferenceFragmentCompat {
    private static final String TAG = GAPreferenceFragment.class.getSimpleName();

    protected GaService mService;

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        final GreenAddressApplication app;
        app = ((GreenAddressApplication) getActivity().getApplication());
        mService = app.mService;
    }

    private static final Preference.OnPreferenceChangeListener onPreferenceChanged =
        new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(final Preference preference, final Object value) {
            preference.setSummary(value.toString());
            return true;
        }
    };

    protected void bindPreferenceSummaryToValue(final Preference preference) {
        preference.setOnPreferenceChangeListener(onPreferenceChanged);
        // Trigger the listener immediately with the preference's
        // current value.
        final String currentVal = mService.cfg().getString(preference.getKey(), "");
        onPreferenceChanged.onPreferenceChange(preference, currentVal);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            startActivity(new Intent(getActivity(), SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected < T > T find(final String preferenceName) {
        return (T) findPreference(preferenceName);
    }

    protected void removePreference(final Preference pref) {
        if (pref != null)
            getPreferenceScreen().removePreference(pref);
    }

    protected void removePreference(final String preferenceName) {
        removePreference(findPreference(preferenceName));
    }

    protected boolean verifyServiceOK() {
        if (mService == null || !mService.getConnectionManager().isPostLogin()) {
            // If we are restored and our service has not been destroyed, its
            // state is unreliable and our parent activity should shortly
            // be calling finish(). Avoid accessing the service in this case,
            // and help the activity along in case it needs prompting to die.
            final Activity activity = getActivity();
            if (activity != null) {
                UI.toast(activity, R.string.id_unable_to_contact_the_green, Toast.LENGTH_LONG);
                activity.finish();
            }
            return false;
        }
        return true;
    }

    protected void logout() {
        ((LoggedActivity) getActivity()).logout();
    }

    protected boolean openURI(final String uri) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
        return false;
    }
}
