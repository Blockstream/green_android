package com.greenaddress.greenbits.ui;

import android.content.Intent;
import android.util.Log;

import com.greenaddress.greenapi.ConnectionManager;

import java.util.Observable;
import java.util.Observer;

public abstract class LoggedActivity extends GaActivity implements Observer {

    private boolean goingFirstScreen = false;

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
            toFirstScreen();
        }
    }

    private void toFirstScreen() {
        // FIXME: Should pass flag to activity so it shows it was forced logged out
        if (!goingFirstScreen) {
            goingFirstScreen = true;
            Intent intent = new Intent(this, FirstScreenActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finishOnUiThread();
        }
    }

    public void logout() {
        startLoading();
        mService.getConnectionManager().deleteObserver(this);
        mService.getExecutor().execute(() -> {
            // When explicitly logging out, treat this as though the user
            // cancelled PIN entry, i.e. Don't jump straight to the PIN
            mService.setUserCancelledPINEntry(true);
            mService.disconnect();
            toFirstScreen();
        });
    }

}
