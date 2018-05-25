package com.greenaddress.greenbits.ui;

import android.content.Intent;

public abstract class LoginActivity extends GaActivity {

    protected void onLoginSuccess() {
        // After login succeeds, show system messaages if there are any
        final Intent intent;
        if (mService.isWatchOnly() || mService.getNextSystemMessageId() == 0)
            intent = new Intent(LoginActivity.this, TabbedMainActivity.class);
        else
            intent = new Intent(LoginActivity.this, MessagesActivity.class);
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
