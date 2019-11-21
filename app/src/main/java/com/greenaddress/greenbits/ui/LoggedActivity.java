package com.greenaddress.greenbits.ui;

import android.content.Intent;
import android.icu.util.DateInterval;
import android.view.MotionEvent;
import android.widget.Toast;

import com.greenaddress.greenapi.ConnectionManager;
import com.greenaddress.greenapi.model.ToastObservable;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;

import java.util.Observable;
import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;

public abstract class LoggedActivity extends GaActivity implements Observer {

    private boolean mChangingActivity = false;
    private Timer mTimer = new Timer();
    private long mStart = System.currentTimeMillis();

    @Override
    public void onResume() {
        super.onResume();
        if (mStart + delayLogoutTimer() < System.currentTimeMillis()) {
            logout();
            return;
        }
        startLogoutTimer();

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
        stopLogoutTimer();
        mStart = System.currentTimeMillis();

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

    @Override
    public boolean dispatchTouchEvent(final MotionEvent ev) {
        stopLogoutTimer();
        startLogoutTimer();
        return super.dispatchTouchEvent(ev);
    }

    private int delayLogoutTimer() {
        if (getModel() != null && getModel().getSettings() != null) {
            return getModel().getSettings().getAltimeout()  * 60 * 1000;
        }
        final String altimeString = cfg().getString(PrefKeys.ALTIMEOUT, "5");
        return Integer.parseInt(altimeString) * 60 * 1000;
    }

    private void startLogoutTimer() {
        stopLogoutTimer();
        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                logout();
            }
        }, delayLogoutTimer());
        mTimer = timer;
    }

    private void stopLogoutTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer.purge();
        }
    }
}
