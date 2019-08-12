package com.greenaddress.greenbits.ui;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.greenaddress.greenapi.ConnectionManager;
import com.greenaddress.greenapi.data.BalanceData;
import com.greenaddress.greenapi.model.ToastObservable;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;

import java.util.Observable;
import java.util.Observer;

import static com.greenaddress.greenbits.ui.TabbedMainActivity.REQUEST_BITCOIN_URL_SEND;

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
            final Intent intent = GaActivity.createToFirstIntent(this, mService);
            startActivity(intent);
            finishOnUiThread();
        }
    }
}
