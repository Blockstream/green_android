package com.greenaddress.greenbits.ui.preferences;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.widget.Toast;

import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.GreenAddressApplication;
import com.greenaddress.greenbits.ui.UI;
import com.greenaddress.greenbits.ui.FirstScreenActivity;


// Our GaPreferenceActivity derived classes aren't exported publicly, so the
// only way they can be created is from internal GaActivity derived activities.
// This means we always have our service available and don't need to check it,
// except when resuming the activity where the service may have been destroyed.
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
               NetworkPreferenceFragment.class.getName().equals(fragmentName) ||
               TwoFactorPreferenceFragment.class.getName().equals(fragmentName);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() != android.R.id.home)
            return false;
        finish();
        return true;
    }

    private GreenAddressApplication getGAApp() {
        return (GreenAddressApplication) getApplication();
    }

    @Override
    final public void onPause() {
        super.onPause();
        final GaService service = getGAApp().mService;
        service.decRef();
    }

    @Override
    final public void onResume() {
        super.onResume();
        final GaService service = getGAApp().mService;
        if (service != null)
            service.incRef();
        if (service == null || service.isForcedOff()) {
            // FIXME: Should pass flag to activity so it shows it was forced logged out
            startActivity(new Intent(this, FirstScreenActivity.class));
            finish();
        }
    }

    public void toast(final String s) {
        UI.toast(this, s, Toast.LENGTH_LONG);
    }
    public void toast(final int id) {
        UI.toast(this, id, Toast.LENGTH_LONG);
    }
}
