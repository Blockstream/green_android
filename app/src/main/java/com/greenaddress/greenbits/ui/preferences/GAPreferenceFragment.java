package com.greenaddress.greenbits.ui.preferences;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.MenuItem;

import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.GreenAddressApplication;

public class GAPreferenceFragment extends PreferenceFragment {
    protected GaService gaService = null;
    GreenAddressApplication gApp = null;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gApp = ((GreenAddressApplication)getActivity().getApplication());
        gaService = gApp.gaService;
    }

    private static final Preference.OnPreferenceChangeListener onPreferenceChanged = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(final Preference preference, final Object value) {
            preference.setSummary(value.toString());
            return true;
        }
    };

    static void bindPreferenceSummaryToValue(final Preference preference) {
        preference.setOnPreferenceChangeListener(onPreferenceChanged);
        // Trigger the listener immediately with the preference's
        // current value.
        onPreferenceChanged.onPreferenceChange(preference, PreferenceManager
                .getDefaultSharedPreferences(preference.getContext())
                .getString(preference.getKey(), ""));
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();
        if (id == android.R.id.home) {
            startActivity(new Intent(getActivity(), SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
