package com.greenaddress.greenbits.ui.preferences;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;

import com.greenaddress.Bridge;
import com.greenaddress.greenapi.Session;
import com.greenaddress.greenbits.ui.UI;


// Our GaPreferenceActivity derived classes aren't exported publicly, so the
// only way they can be created is from internal GaActivity derived activities.
// This means we always have our service available and don't need to check it,
// except when resuming the activity where the service may have been destroyed.
@Deprecated
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

    @Override
    final public void onPause() {
        super.onPause();
    }

    @Override
    final public void onResume() {
        super.onResume();

        if (!Bridge.INSTANCE.isSessionConnected() ||
                getSession() == null ||
                getSession().getSettings() == null) {

            Bridge.INSTANCE.navigateToLogin(this, Bridge.INSTANCE.getActiveWalletId());
            return;
        }
    }

    protected void logout() {
        Bridge.INSTANCE.navigateToLogin(this, null);
    }

    public void toast(final int id) {
        UI.toast(this, id, Toast.LENGTH_LONG);
    }

    public Session getSession() {
        return Session.getSession();
    }
}
