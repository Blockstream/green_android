package com.greenaddress.greenbits.ui.preferences;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import androidx.fragment.app.Fragment;

@Deprecated
public class SettingsActivity extends GaPreferenceActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(android.R.color.transparent));

        Fragment fragment;

        if(savedInstanceState == null) {
            try {
                if (getIntent().hasExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT)) {
                    final String preference = getIntent().getStringExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT);

                    if (preference.equals(SPVPreferenceFragment.class.getName()))
                        fragment = new SPVPreferenceFragment();
                    else
                        fragment = new GeneralPreferenceFragment();

                } else {
                    // Moved from TabbedMainActivity
                    if (getSession().isTwoFAReset()){
                        fragment = new ResetActivePreferenceFragment();
                        fragment.setArguments(getIntent().getExtras());
                    }
                    else if (getSession().isWatchOnly())
                        fragment = new WatchOnlyPreferenceFragment();
                    else
                        fragment = new GeneralPreferenceFragment();
                }

                getSupportFragmentManager().beginTransaction().add(android.R.id.content, fragment).commit();

            } catch (final Exception e) {
                e.printStackTrace();
                finish();
            }
        }
    }
}
