package com.greenaddress.greenbits.ui.preferences;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SettingsActivity extends GaPreferenceActivity {
    private Fragment fragment;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(android.R.color.transparent));
        final String preference;
        try {
            preference = getIntent().getStringExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT);
        } catch (final Exception e) {
            e.printStackTrace();
            return;
        }

        if (getGAApp().mService == null || getGAApp().getModel() == null) {
            logout();
            return;
        }

        if (preference.equals(SPVPreferenceFragment.class.getName()))
            fragment = new SPVPreferenceFragment();
        else if (preference.equals(PinPreferenceFragment.class.getName()))
            fragment = new PinPreferenceFragment();
        else
            fragment = new GeneralPreferenceFragment();
        getSupportFragmentManager().beginTransaction().add(android.R.id.content, fragment).commit();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (fragment != null)
            fragment.onActivityResult(requestCode, resultCode, data);
    }
}
