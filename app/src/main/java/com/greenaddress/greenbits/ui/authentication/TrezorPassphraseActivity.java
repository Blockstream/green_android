package com.greenaddress.greenbits.ui.authentication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.greenaddress.greenbits.ui.GaActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;

@Deprecated
public class TrezorPassphraseActivity extends GaActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trezor_passphrase);
        setTitleBackTransparent();

        final TextView value = UI.find(this, R.id.trezorPassphraseValue);
        final TextView valueConfirm = UI.find(this, R.id.trezorPassphraseValueConfirm);

        final Button ok = UI.find(this, R.id.trezorPassphraseOk);
        ok.setOnClickListener(view -> {
            if (!value.getText().toString().equals(valueConfirm.getText().toString())) {
                UI.toast(this, R.string.id_error_passphrases_do_not_match, Toast.LENGTH_LONG);
            } else {
                Intent intent = getIntent();
                intent.putExtra(String.valueOf(GaActivity.HARDWARE_PASSPHRASE_REQUEST), value.getText().toString());
                setResult(Activity.RESULT_OK, intent);
                finishOnUiThread();
            }
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
