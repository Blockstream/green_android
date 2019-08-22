package com.greenaddress.greenbits.ui.authentication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.greenaddress.greenbits.ui.GaActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;


public class TrezorPinActivity extends GaActivity {
    private StringBuffer value = new StringBuffer();

    @Override
    protected void onCreateWithService(Bundle savedInstanceState) {
        setContentView(R.layout.activity_trezor_pin_dialog);
        setTitleBackTransparent();
        final TextView textView = UI.find(this, R.id.asteriskTextView);
        final Button[] buttons = new Button[] {
            // upside down
            UI.find(this, R.id.trezorPinButton7),
            UI.find(this, R.id.trezorPinButton8),
            UI.find(this, R.id.trezorPinButton9),
            UI.find(this, R.id.trezorPinButton4),
            UI.find(this, R.id.trezorPinButton5),
            UI.find(this, R.id.trezorPinButton6),
            UI.find(this, R.id.trezorPinButton1),
            UI.find(this, R.id.trezorPinButton2),
            UI.find(this, R.id.trezorPinButton3)
        };
        for (int i = 0; i < buttons.length; ++i) {
            final int ii = i;
            buttons[i].setOnClickListener(view -> {
                if (value.length() < 9) {
                    value.append(String.valueOf(ii+1));
                    textView.setText(String.format("%s *",textView.getText()));
                }
            });
        }
        final ImageView button = UI.find(this, R.id.eraseButton);
        button.setOnClickListener(view -> {
            if (value.length() > 0) {
                value.deleteCharAt(value.length()-1);
                final CharSequence text = textView.getText();
                textView.setText(text.subSequence(0,text.length()-2));
            }
        });

        final Button next = UI.find(this, R.id.trezorPinNextButton);
        next.setOnClickListener(view -> {
            Intent intent = getIntent();
            intent.putExtra(String.valueOf(GaActivity.HARDWARE_PIN_REQUEST),value.toString());
            setResult(Activity.RESULT_OK, intent);
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
