package com.greenaddress.greenbits.ui;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import com.greenaddress.greenbits.ui.onboarding.TermsActivity;
import com.greenaddress.greenbits.ui.preferences.NetworkPreferenceFragment;
import com.greenaddress.greenbits.ui.preferences.SettingsActivity;


public class FirstScreenActivity extends LoginActivity implements NetworkSettingsFragment.Listener {
    private Button mSelectNetwork;

    @Override
    protected int getMainViewId() { return R.layout.activity_first_screen; }

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {
        UI.mapClick(this, R.id.firstLogInButton, new Intent(this, MnemonicActivity.class));
        UI.mapClick(this, R.id.firstSignUpButton, new Intent(this, TermsActivity.class));
        UI.mapClick(this, R.id.settingsButton, view -> openNetworkSettings());
        UI.mapClick(this, R.id.watchOnlyButton, new Intent(this, WatchOnlyLoginActivity.class) );
        UI.mapClick(this, R.id.goToPinButton, new Intent(this, PinActivity.class) );
        mSelectNetwork = UI.find(this, R.id.settingsButton);
    }

    private void openNetworkSettings() {
        final NetworkSettingsFragment dialogFragment = new NetworkSettingsFragment();
        dialogFragment.setListener(this);
        dialogFragment.show(getSupportFragmentManager(), dialogFragment.getTag());
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.preauth_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
        case R.id.watchonly_preference:
            startActivity(new Intent(FirstScreenActivity.this, WatchOnlyLoginActivity.class));
            return true;
        case R.id.network_preferences:
            final Intent intent = new Intent(this, SettingsActivity.class);
            intent.putExtra( PreferenceActivity.EXTRA_SHOW_FRAGMENT, NetworkPreferenceFragment.class.getName() );
            startActivity(intent);
            return true;
            //case R.id.action_settings:
            //    return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResumeWithService() {
        mSelectNetwork.setText(mService.getNetwork().getName());
        UI.showIf( mService.hasPin(), UI.find(this,R.id.goToPinButton));
        if (!mService.getUserCancelledPINEntry()) {
            mService.setUserCancelledPINEntry(false);
            checkPinExist();
        }
    }

    @Override
    public void onSelectNetwork() {
        mSelectNetwork.setText(mService.getNetwork().getName());
        UI.showIf( mService.hasPin(), UI.find(this,R.id.goToPinButton));
        checkPinExist();
    }
}
