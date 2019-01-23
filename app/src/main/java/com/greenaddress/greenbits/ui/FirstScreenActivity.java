package com.greenaddress.greenbits.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.greenaddress.greenbits.ui.onboarding.TermsActivity;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;

public class FirstScreenActivity extends LoginActivity implements NetworkSettingsFragment.Listener {
    private Button mSelectNetwork;

    @Override
    protected int getMainViewId() { return R.layout.activity_first_screen; }

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {
        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(android.R.color.transparent));
        setTitle("");

        final Button firstLogInButton = findViewById(R.id.firstLogInButton);
        firstLogInButton.setOnClickListener(v -> askConfirmation(
                new Intent(this, MnemonicActivity.class),
                R.string.id_there_is_already_a_pin_set_for));
        final Button firstSignUpButton = findViewById(R.id.firstSignUpButton);
        firstSignUpButton.setOnClickListener(v -> askConfirmation(
                new Intent(this, TermsActivity.class),
                R.string.id_there_is_already_a_pin_set_for));

        mSelectNetwork = UI.find(this, R.id.settingsButton);
        mSelectNetwork.setOnClickListener(v -> openNetworkSettings());
    }

    private void askConfirmation(final Intent intent, final int message) {
        if(mService.hasPin()) {
            UI.popup(this, R.string.id_warning)
                    .content(getString(message, mService.getNetwork().getNetwork()))
                    .onPositive((dialog, which) -> startActivity(intent)).build().show();
        } else {
            startActivity(intent);
        }
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
        mService.setPinJustSaved(false);
    }

    @Override
    public void onSelectNetwork() {
        mSelectNetwork.setText(mService.getNetwork().getName());
        invalidateOptionsMenu();
        checkPinExist();
    }
}
