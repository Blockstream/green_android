package com.greenaddress.greenbits.ui.preferences;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;

import com.greenaddress.greenbits.GreenAddressApplication;
import com.greenaddress.greenbits.ui.GaActivity;
import com.greenaddress.greenbits.ui.UI;
import com.greenaddress.greenbits.ui.authentication.FirstScreenActivity;


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

    /*@Override
       public boolean onIsMultiPane() {
        final int layout = getResources().getConfiguration().screenLayout;
        return (layout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
       }*/

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(final String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName) ||
               GAPreferenceFragment.class.getName().equals(fragmentName) ||
               GeneralPreferenceFragment.class.getName().equals(fragmentName) ||
               SPVPreferenceFragment.class.getName().equals(fragmentName);
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
    }

    @Override
    final public void onResume() {
        super.onResume();
    }

    protected void logout() {
        final Intent intent = GaActivity.createToFirstIntent(this);
        startActivity(intent);
    }


    public void toast(final int id) {
        UI.toast(this, id, Toast.LENGTH_LONG);
    }
}
