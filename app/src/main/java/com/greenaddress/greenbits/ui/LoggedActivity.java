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
    public void onResume() {
        super.onResume();
        if (getConnectionManager() == null || getModel() == null) {
            toFirst();
            return;
        }
        if (getConnectionManager().isDisconnected()) {
            toFirst();
            return;
        }
        getConnectionManager().addObserver(this);
        getModel().getToastObservable().addObserver(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getConnectionManager() == null || getModel() == null)
            return;
        getConnectionManager().deleteObserver(this);
        getModel().getToastObservable().deleteObserver(this);
    }

    @Override
    public void update(final Observable observable, final Object o) {
        if (observable instanceof ConnectionManager) {
            final ConnectionManager cm = getConnectionManager();
            if (cm.isLoginRequired()) {
                cm.connect(this);
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
        if (getConnectionManager() == null || getModel() == null) {
            toFirst();
            return;
        }
        getConnectionManager().deleteObserver(this);
        getGAApp().getExecutor().execute(() -> {
            startLoading();
            getConnectionManager().disconnect();
            toFirst();
            stopLoading();
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
