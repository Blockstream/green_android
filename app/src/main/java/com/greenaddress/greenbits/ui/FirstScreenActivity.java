package com.greenaddress.greenbits.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import com.greenaddress.greenbits.ui.onboarding.TermsActivity;

public class FirstScreenActivity extends LoginActivity implements NetworkSettingsFragment.Listener {
    private Button mSelectNetwork;

    @Override
    protected int getMainViewId() { return R.layout.activity_first_screen; }

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {
        UI.mapClick(this, R.id.firstLogInButton, new Intent(this, MnemonicActivity.class));
        UI.mapClick(this, R.id.firstSignUpButton, new Intent(this, TermsActivity.class));
        UI.mapClick(this, R.id.settingsButton, view -> openNetworkSettings());
        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(android.R.color.transparent));
        setTitle("");
        mSelectNetwork = UI.find(this, R.id.settingsButton);
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
            startActivity(new Intent(this, WatchOnlyLoginActivity.class));
            return true;
        case R.id.enter_pin:
            startActivity(new Intent(this, PinActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
        menu.findItem(R.id.enter_pin).setEnabled( mService.hasPin());
        return true;
    }

    @Override
    public void onResumeWithService() {
        mSelectNetwork.setText(mService.getNetwork().getName());
    }

    @Override
    public void onSelectNetwork() {
        mSelectNetwork.setText(mService.getNetwork().getName());
        invalidateOptionsMenu();
        checkPinExist();
    }
}
