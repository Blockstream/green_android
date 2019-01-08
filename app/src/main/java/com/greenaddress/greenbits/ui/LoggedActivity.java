package com.greenaddress.greenbits.ui;

import android.content.Intent;
import android.util.Log;

import com.greenaddress.greenapi.ConnectionManager;

import java.util.Observable;
import java.util.Observer;

public abstract class LoggedActivity extends GaActivity implements Observer {

    private boolean mChangingActivity = false;

    @Override
    protected void onResumeWithService() {
        super.onResumeWithService();
        if (mService == null)
            return;
        kickMeOutIfNotLogged();
        mService.getConnectionManager().addObserver(this);
    }

    @Override
    protected void onPauseWithService() {
        super.onPauseWithService();
        if (mService == null)
            return;
        mService.getConnectionManager().deleteObserver(this);
    }

    @Override
    public void update(final Observable observable, final Object o) {
        if (observable instanceof ConnectionManager) {
            kickMeOutIfNotLogged();
        }
    }

    private void kickMeOutIfNotLogged() {
        if (!mService.getConnectionManager().isPostLogin()) {
            Log.i("LoggedActivity","not logged any more kicking out");
            toFirstOrPinScreen(mService.getConnectionManager().isLoginWithPin());
        }
    }

    private void toScreen(final Intent intent) {
        if (!mChangingActivity) {
            mChangingActivity = true;
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finishOnUiThread();
        }
    }

    public void logout() {
        startLoading();
        mService.getConnectionManager().deleteObserver(this);
        mService.getExecutor().execute(() -> {
            final boolean loggedWithPin = mService.getConnectionManager().isLoginWithPin();
            mService.disconnect();
            toFirstOrPinScreen(loggedWithPin);
        });
    }

    private void toFirstOrPinScreen(final boolean toPin) {
        if (toPin)
            toScreen(new Intent(this, PinActivity.class));
        else
            toScreen(new Intent(this, FirstScreenActivity.class));
    }

}
