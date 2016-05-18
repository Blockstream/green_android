package com.greenaddress.greenbits.ui.preferences;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

import com.greenaddress.greenbits.ConnectivityObservable;
import com.greenaddress.greenbits.GreenAddressApplication;
import com.greenaddress.greenbits.ui.FirstScreenActivity;


// Our GaPreferenceActivity derived classes aren't exported publically, so the
// only way they can be created is from internal GaActivity derived activities.
// This means we always have our service available and don't need to check it.
//
// We only need to check that we havent been logged off when resuming the
// session, and update the session ref count correctly.
public abstract class GaPreferenceActivity extends AppCompatPreferenceActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    private void setupActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onIsMultiPane() {
        final int layout = getResources().getConfiguration().screenLayout;
        return (layout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(final String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName) ||
               GAPreferenceFragment.class.getName().equals(fragmentName) ||
               GeneralPreferenceFragment.class.getName().equals(fragmentName) ||
               SPVPreferenceFragment.class.getName().equals(fragmentName) ||
               ProxyPreferenceFragment.class.getName().equals(fragmentName) ||
               TwoFactorPreferenceFragment.class.getName().equals(fragmentName);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() != android.R.id.home)
            return false;
        finish();
        return true;
    }

    protected GreenAddressApplication getGAApp() {
        return (GreenAddressApplication) getApplication();
    }

    @Override
    final public void onPause() {
        super.onPause();
        getGAApp().getConnectionObservable().decRef();
    }

    @Override
    final public void onResume() {
        super.onResume();
        final ConnectivityObservable.ConnectionState cs = getGAApp().getConnectionObservable().incRef();
        if (cs.mForcedLogout || cs.mForcedTimeout) {
            // FIXME: Should pass flag to activity so it shows it was forced logged out
            final Intent firstScreenActivity = new Intent(this, FirstScreenActivity.class);
            startActivity(firstScreenActivity);
            finish();
        }
    }
}
