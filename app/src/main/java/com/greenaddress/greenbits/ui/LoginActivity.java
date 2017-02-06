package com.greenaddress.greenbits.ui;

import android.content.Intent;

public abstract class LoginActivity extends GaActivity {

    protected void onLoginSuccess() {
        final Intent intent = new Intent(LoginActivity.this, TabbedMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finishOnUiThread();
    }

    @Override
    protected void onResumeWithService() {
        if (mService.isLoggedOrLoggingIn()) {
            // already logged in, could be from different app via intent
            onLoginSuccess();
        }
    }
}
