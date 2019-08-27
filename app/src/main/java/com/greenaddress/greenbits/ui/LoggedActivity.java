package com.greenaddress.greenbits.ui;

import android.content.Intent;
import android.widget.Toast;

import com.greenaddress.greenapi.ConnectionManager;
import com.greenaddress.greenapi.model.ToastObservable;

import java.util.Observable;
import java.util.Observer;

public abstract class LoggedActivity extends GaActivity implements Observer {

    private boolean mChangingActivity = false;

    @Override
    protected void onResumeWithService() {
        super.onResumeWithService();
        if (mService == null || mService.getModel() == null) {
            toFirst();
            return;
        }
        if (mService.isDisconnected()) {
            toFirst();
            return;
        }
        mService.getConnectionManager().addObserver(this);
        mService.getModel().getToastObservable().addObserver(this);
    }

    @Override
    protected void onPauseWithService() {
        super.onPauseWithService();
        if (mService == null || mService.getModel() == null)
            return;
        mService.getConnectionManager().deleteObserver(this);
        mService.getModel().getToastObservable().deleteObserver(this);
    }

    @Override
    public void update(final Observable observable, final Object o) {
        if (observable instanceof ConnectionManager) {
            final ConnectionManager cm = mService.getConnectionManager();
            if (cm.isLoginRequired()) {
                cm.login(this, cm.getHWDeviceData(), cm.getHWResolver());
            } else if (cm.isDisconnected()) {
                toFirst();
            }
        } else if (observable instanceof ToastObservable) {
            final ToastObservable tObs = (ToastObservable) observable;
            UI.toast(this, tObs.getMessage(getResources()), Toast.LENGTH_LONG);
        }
    }

    public void logout() {
        if (mService == null || getModel() == null) {
            toFirst();
            return;
        }
        mService.getConnectionManager().deleteObserver(this);
        mService.getExecutor().execute(() -> {
            mService.disconnect();
            toFirst();
        });
    }

    public void toFirst() {
        if (!mChangingActivity) {
            mChangingActivity = true;
            final Intent intent = GaActivity.createToFirstIntent(this);
            startActivity(intent);
            finishOnUiThread();
        }
    }
}
