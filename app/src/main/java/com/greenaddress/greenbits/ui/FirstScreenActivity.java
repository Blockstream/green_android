package com.greenaddress.greenbits.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TextView.BufferType;

import com.greenaddress.greenbits.ui.onboarding.TermsActivity;

public class FirstScreenActivity extends LoginActivity {

    public static final int NETWORK_SELECTOR_REQUEST = 51341;
    private Button mSelectNetwork;
    private TextView mWalletDetected;

    @Override
    protected int getMainViewId() { return R.layout.activity_first_screen; }

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {
        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(android.R.color.transparent));
        setTitle("");

        final Button firstLogInButton = findViewById(R.id.firstLogInButton);
        firstLogInButton.setOnClickListener(v -> askConfirmation(
                                                new Intent(this, MnemonicActivity.class),
                                                R.string.id_green_only_supports_one_pin_for));
        final Button firstSignUpButton = findViewById(R.id.firstSignUpButton);
        firstSignUpButton.setOnClickListener(v -> askConfirmation(
                                                 new Intent(this, TermsActivity.class),
                                                 R.string.id_green_only_supports_one_pin_for));

        mSelectNetwork = UI.find(this, R.id.settingsButton);
        mSelectNetwork.setOnClickListener(v -> { startActivityForResult(new Intent(this, NetworkSettingsActivity.class),
                                                                        NETWORK_SELECTOR_REQUEST); });

        mWalletDetected = UI.find( this, R.id.walletDetected);
        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(UI.getColoredString(getString(R.string.id_a_wallet_is_detected_on_this),
                                           getResources().getColor(R.color.grey_light)));
        builder.append(" ");
        builder.append(UI.getColoredString(getString(R.string.id_log_in),
                                           getResources().getColor(R.color.green)));
        mWalletDetected.setText(builder, BufferType.SPANNABLE);
        mWalletDetected.setOnClickListener(v -> startActivity(new Intent(this, PinActivity.class)));
    }

    private void askConfirmation(final Intent intent, final int message) {
        if (mService.hasPin()) {
            UI.popup(this, R.string.id_warning)
            .content(getString(message))
            .onPositive((dialog, which) -> startActivity(intent)).build().show();
        } else {
            startActivity(intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.preauth_menu, menu);
        menu.findItem(R.id.watchonly_preference).setVisible(!mService.isLiquid());
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
    }
}
