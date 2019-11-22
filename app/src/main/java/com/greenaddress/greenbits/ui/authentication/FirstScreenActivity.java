package com.greenaddress.greenbits.ui.authentication;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenbits.AuthenticationHandler;
import com.greenaddress.greenbits.ui.LoginActivity;
import com.greenaddress.greenbits.ui.NetworkSettingsActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;
import com.greenaddress.greenbits.ui.onboarding.InfoActivity;

public class FirstScreenActivity extends LoginActivity {

    public static final int NETWORK_SELECTOR_REQUEST = 51341;

    @Override
    protected int getMainViewId() { return R.layout.activity_first_screen; }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(android.R.color.transparent));
        setTitle("");
    }


    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.preauth_menu, menu);
        menu.findItem(R.id.action_watchonly).setVisible(!getGAApp().getCurrentNetworkData().getLiquid());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
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
    public void onResume() {
        super.onResume();
        onSelectNetwork();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == NETWORK_SELECTOR_REQUEST && resultCode == RESULT_OK)
            onSelectNetwork();
    }

    public void onSelectNetwork() {
        final NetworkData networkData = getGAApp().getCurrentNetworkData();
        invalidateOptionsMenu();

        final Button loginButton = findViewById(R.id.loginButton);
        final Button restoreButton = findViewById(R.id.restoreButton);
        final Button selectNetworkButton = UI.find(this, R.id.settingsButton);

        restoreButton.setVisibility(AuthenticationHandler.hasPin(this) ? View.GONE : View.VISIBLE);
        restoreButton.setOnClickListener(v -> startActivity(new Intent(this, MnemonicActivity.class)));
        loginButton.setText(AuthenticationHandler.hasPin(this) ? R.string.id_log_in : R.string.id_create_new_wallet);
        final Intent loginButtonIntent = AuthenticationHandler.hasPin(this) ?
                                         new Intent(this, PinActivity.class) :
                                         new Intent(this, InfoActivity.class);
        loginButton.setOnClickListener(v -> startActivity(loginButtonIntent));

        selectNetworkButton.setText(networkData.getName());
        selectNetworkButton.setOnClickListener(v -> { startActivityForResult(new Intent(this,
                                                                                        NetworkSettingsActivity.class),
                                                                             NETWORK_SELECTOR_REQUEST); });
    }
}
