package com.greenaddress.greenbits.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.MenuItem;

import com.fasterxml.jackson.databind.JsonNode;

import static com.greenaddress.greenbits.ui.ScanActivity.INTENT_STRING_TX;

public class SendActivity extends LoggedActivity implements SendInputFragment.OnCallbackListener {
    private static final String TAG = SendActivity.class.getSimpleName();

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {
        setContentView(R.layout.activity_send);
        UI.preventScreenshots(this);

        setTitleBackTransparent();

        // Get parameters from Intent
        try {
            final Intent intent = getIntent();
            final String tx = intent.getStringExtra(INTENT_STRING_TX);

            // Show input fragment
            final Bundle bundle = new Bundle();
            bundle.putString(INTENT_STRING_TX, tx);

            final Fragment fragment = new SendInputFragment();
            fragment.setArguments(bundle);

            getSupportFragmentManager()
            .beginTransaction()
            .replace(android.R.id.content, fragment)
            .commit();

        } catch (final Exception e) {
            e.printStackTrace();
            return;
        }
    }

    @Override
    public void onBackPressed() {
        final int fragments = getSupportFragmentManager().getBackStackEntryCount();
        if (fragments == 1)
            getSupportFragmentManager().popBackStack();
        else
            super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            onBackPressed();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onFinish(final JsonNode transactionData) {
        // Open next fragment
        final Bundle bundle = new Bundle();
        bundle.putString("transaction", transactionData.toString());
        if (mService.getConnectionManager().isHW())
            bundle.putString("hww", mService.getConnectionManager().getHWDeviceData().toString());

        final Fragment fragment = new SendConfirmFragment();
        fragment.setArguments(bundle);

        getSupportFragmentManager()
        .beginTransaction()
        .replace(android.R.id.content, fragment, fragment.getTag())
        .addToBackStack(fragment.getTag())
        .commit();
    }
}
