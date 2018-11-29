package com.greenaddress.greenbits.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.greenaddress.greenbits.ui.GaActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.TabbedMainActivity;
import com.greenaddress.greenbits.ui.UI;

public class SuccessActivity extends GaActivity implements View.OnClickListener {

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {
        setContentView(R.layout.activity_onboarding_success);
        setTitleBackTransparent();
        setTitle("");
    }

    @Override
    protected void onResumeWithService() {
        UI.mapClick(this, R.id.securityButton, this);
        UI.mapClick(this, R.id.continueButton, this);
    }

    @Override
    protected void onPauseWithService() {
        UI.unmapClick(UI.find(this, R.id.securityButton));
        UI.unmapClick(UI.find(this, R.id.continueButton));
    }

    @Override
    public void onClick(final View view) {
        // TODO check connection manager status
        /*if (!mService.getConnectionManager().isConnected()) {
            toast(R.string.id_you_are_not_connected_please);
            return;
           }*/
        final Intent intent;
        switch (view.getId()) {
        case R.id.continueButton:
            intent = new Intent(this, TabbedMainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            break;
        case R.id.securityButton:
            intent = new Intent(this, SecurityActivity.class);
            intent.putExtra("from_onboarding",true);
            break;
        default:
            return;
        }
        startActivity(intent);
        finish();
    }
}
