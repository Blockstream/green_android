package com.greenaddress.greenbits.ui.preferences;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.v4.app.Fragment;

public class SettingsActivity extends GaPreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(android.R.color.transparent));
        final String preference;
        try {
            preference = getIntent().getStringExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT);
        } catch (final Exception e) {
            e.printStackTrace();
            return;
        }

        if (getGAApp().mService == null || getGAApp().mService.getModel() == null) {
            logout();
            return;
        }

        final Fragment fragment;
        if (preference.equals(SPVPreferenceFragment.class.getName()))
            fragment = new SPVPreferenceFragment();
        else if (preference.equals(NetworkPreferenceFragment.class.getName()))
            fragment = new NetworkPreferenceFragment();
        else
            fragment = new GeneralPreferenceFragment();
        getSupportFragmentManager().beginTransaction().add(android.R.id.content, fragment).commit();
    }
}
