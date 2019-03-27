package com.greenaddress.greenbits.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

public class TrezorPassphraseActivity extends GaActivity {

    @Override
    protected void onCreateWithService(Bundle savedInstanceState) {
        setContentView(R.layout.activity_trezor_passphrase);
        setTitleBackTransparent();

        final TextView value = UI.find(this, R.id.trezorPassphraseValue);

        final Button ok = UI.find(this, R.id.trezorPassphraseOk);
        ok.setOnClickListener(view -> {
            Intent intent = getIntent();
            intent.putExtra( String.valueOf(GaActivity.HARDWARE_PASSPHRASE_REQUEST), value.getText().toString());
            setResult(Activity.RESULT_OK, intent);
            finishOnUiThread();
        });

        final Button cancel = UI.find(this, R.id.trezorPassphraseCancel);
        cancel.setOnClickListener(view -> {
            finishOnUiThread();
        });
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
}
