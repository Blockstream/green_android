package com.greenaddress.greenbits;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.greenaddress.greenbits.ui.TabbedMainActivity;

import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ConnectivityObservable extends Observable {

    static int RECONNECT_TIMEOUT = 6000;
    static int RECONNECT_TIMEOUT_MAX = 50000;
    private final ScheduledThreadPoolExecutor ex = new ScheduledThreadPoolExecutor(1);
    private ScheduledFuture<Object> disconnectTimeout;
    private GaService service;
    private State state = State.OFFLINE;
    private boolean forcedLoggedout = false;
    private boolean forcedTimeoutout = false;
    private final BroadcastReceiver mNetBroadReceiver = new BroadcastReceiver() {
        public void onReceive(final Context context, final Intent intent) {
            checkNetwork();
        }
    };

    public void setService(final GaService service) {
        this.service = service;
        checkNetwork();
        service.getApplicationContext().registerReceiver(this.mNetBroadReceiver,
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    public void setForcedLoggedOut() {
        this.forcedLoggedout = true;
        setChanged();
        notifyObservers(this.forcedLoggedout);
    }

    public State getState() {
        return state;
    }

    public void setState(final State state) {
        this.state = state;
        if (state == State.LOGGEDIN) {
            this.forcedLoggedout = false;
            this.disconnectTimeout = null;
            this.forcedTimeoutout = false;
        }
        setChanged();
        notifyObservers(state);
    }

    public boolean getIsForcedLoggedOut() {
        return forcedLoggedout;
    }

    public boolean getIsForcedTimeout() {
        return forcedTimeoutout;
    }

    @Override
    public void addObserver(final Observer ob) {
        super.addObserver(ob);
        stopTimer();
        // connect as necessary
        if (service != null && state.equals(State.DISCONNECTED)) {
            service.reconnect();
        }
        if (countObservers() == 1 && service != null) {

            service.getApplicationContext().registerReceiver(this.mNetBroadReceiver,
                    new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }
    }

    @Override
    public synchronized void deleteObserver(final Observer ob) {
        super.deleteObserver(ob);
        if (countObservers() == 0) {
            if (service != null) {
                service.getApplicationContext().unregisterReceiver(mNetBroadReceiver);
            }
            startTimer();
        }
    }

    @Override
    public synchronized void deleteObservers() {
        super.deleteObservers();
        startTimer();
    }

    private void stopTimer() {
        if (service != null) {
            if (disconnectTimeout != null && !disconnectTimeout.isCancelled()) {
                disconnectTimeout.cancel(false);
            }
        }
    }

    private void startTimer() {

        if (service != null) {

            int timeout = 5;
            try {
                timeout = (int) service.getAppearanceValue("altimeout");
            } catch (final Exception e) {
                // not logged in or not set
            }

            disconnectTimeout = ex.schedule(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    forcedTimeoutout = true;
                    service.disconnect(false);
                    setChanged();
                    notifyObservers();
                    return null;
                }
            }, timeout, TimeUnit.MINUTES);
        }
    }

    private void checkNetwork() {
        final boolean isNetworkUp = isNetworkUp();
        boolean stateSet = false;
        if (isNetworkUp) {
            if (state.equals(State.DISCONNECTED) || state.equals(State.OFFLINE)) {
                setState(ConnectivityObservable.State.DISCONNECTED);
                stateSet = true;
                service.reconnect();
            }
        } else {
            setState(ConnectivityObservable.State.DISCONNECTED);
            setState(ConnectivityObservable.State.OFFLINE);
            stateSet = true;
        }
        if (isWiFiUp() && !stateSet) {
            setChanged();
            notifyObservers();
        }
    }

    public boolean isNetworkUp() {
        final ConnectivityManager connectivityManager
                = (ConnectivityManager) service.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    public boolean isWiFiUp() {
        final ConnectivityManager connectivityManager
                = (ConnectivityManager) service.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    public enum State {
        OFFLINE, DISCONNECTED, CONNECTING, CONNECTED, LOGGINGIN, LOGGEDIN
    }
}
