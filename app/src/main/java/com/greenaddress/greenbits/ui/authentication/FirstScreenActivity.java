package com.greenaddress.greenbits.ui.authentication;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TextView.BufferType;

import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.ui.LoginActivity;
import com.greenaddress.greenbits.ui.NetworkSettingsActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.ThemeUtils;
import com.greenaddress.greenbits.ui.UI;
import com.greenaddress.greenbits.ui.onboarding.TermsActivity;

public class FirstScreenActivity extends LoginActivity {

    public static final int NETWORK_SELECTOR_REQUEST = 51341;
    private Button mSelectNetwork;
    private TextView mWalletDetected;

    private int mTheme;

    @Override
    protected int getMainViewId() { return R.layout.activity_first_screen; }

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {
        mTheme = ThemeUtils.getThemeFromNetworkId(GaService.getNetworkFromId(GaService.getCurrentNetworkId(
                                                                                 this)), this, getMetadata());

        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(android.R.color.transparent));
        setTitle("");

        final Button loginButton = findViewById(R.id.loginButton);
        final Button restoreButton = findViewById(R.id.restoreButton);
        if (mService.hasPin()) {
            restoreButton.setVisibility(View.GONE);
        }
        restoreButton.setOnClickListener(v -> startActivity(new Intent(this, MnemonicActivity.class)));
        loginButton.setText(mService.hasPin() ? R.string.id_log_in : R.string.id_create_new_wallet);
        Intent loginButtonIntent = mService.hasPin() ?
                                   new Intent(this, PinActivity.class) :
                                   new Intent(this, TermsActivity.class);
        loginButton.setOnClickListener(v -> startActivity(loginButtonIntent));


        mSelectNetwork = UI.find(this, R.id.settingsButton);
        mSelectNetwork.setOnClickListener(v -> { startActivityForResult(new Intent(this, NetworkSettingsActivity.class),
                                                                        NETWORK_SELECTOR_REQUEST); });

    }


    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.preauth_menu, menu);
        if (mService != null)
            menu.findItem(R.id.action_watchonly).setVisible(!mService.isLiquid());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
        case R.id.action_watchonly:
            startActivity(new Intent(this, WatchOnlyLoginActivity.class));
            return true;
        case R.id.action_restore_temporary:
            final Intent intent = new Intent(this, MnemonicActivity.class);
            intent.putExtra(MnemonicActivity.TEMPORARY_MODE, true);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResumeWithService() {
        onSelectNetwork();
        mService.setPinJustSaved(false);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == NETWORK_SELECTOR_REQUEST && resultCode == RESULT_OK)
            onSelectNetwork();
    }

    public void onSelectNetwork() {
        mSelectNetwork.setText(mService.getNetwork().getName());
        UI.showIf(mService.hasPin(), mWalletDetected);
        invalidateOptionsMenu();

        // Recreate the activity to load the new theme if necessary
        if (mTheme != ThemeUtils.getThemeFromNetworkId(mService.getNetwork(), this, getMetadata())) {
            recreate();
        }
    }
}
