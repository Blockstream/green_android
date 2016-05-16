package com.greenaddress.greenbits;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ConnectivityObservable extends Observable {

    public enum State {
        OFFLINE, DISCONNECTED, CONNECTING, CONNECTED, LOGGINGIN, LOGGEDIN
    }

    static final int RECONNECT_TIMEOUT = 6000;
    static final int RECONNECT_TIMEOUT_MAX = 50000;
    @NonNull private final ScheduledThreadPoolExecutor ex = new ScheduledThreadPoolExecutor(1);
    private ScheduledFuture<Object> disconnectTimeout = null;
    private GaService service;
    @NonNull
    private State state = State.OFFLINE;
    private boolean mForcedLogout = false;
    private boolean mForcedTimeout = false;
    private int mRefCount = 0; // The number of non-paused activities using the session
    @NonNull private final BroadcastReceiver mNetBroadReceiver = new BroadcastReceiver() {
        public void onReceive(final Context context, final Intent intent) {
            checkNetwork();
        }
    };

    public void incRef() {
        assert service != null : "Reference incremented before service created";
        ++mRefCount;
        if (disconnectTimeout != null && !disconnectTimeout.isCancelled())
            disconnectTimeout.cancel(false);
        if (state.equals(State.DISCONNECTED))
            service.reconnect();
    }

    public void decRef() {
        assert service != null : "Reference decremented before service created";
        assert mRefCount > 0 : "Incorrect reference count";
        if (--mRefCount == 0)
            disconnectTimeout = ex.schedule(new Callable<Object>() {
                    @Nullable
                    @Override
                    public Object call() throws Exception {
                        mForcedTimeout = true;
                        service.disconnect(false);
                        doNotify(null);
                        return null;
                    }
            }, service.getAutoLogoutMinutes(), TimeUnit.MINUTES);
    }

    public void setService(@NonNull final GaService service) {
        this.service = service;
        checkNetwork();
        service.getApplicationContext().registerReceiver(this.mNetBroadReceiver,
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    private void doNotify(final Object o) {
         setChanged();
         notifyObservers(o);
    }

    public void setForcedLoggedOut() {
        this.mForcedLogout = true;
        doNotify(this.mForcedLogout);
    }

    @NonNull
    public State getState() {
        return state;
    }

    public void setState(@NonNull final State state) {
        this.state = state;
        if (state == State.LOGGEDIN) {
            mForcedLogout = false;
            mForcedTimeout = false;
        }
        doNotify(state);
    }

    public boolean isForcedOff() {
        return mForcedLogout || mForcedTimeout;
    }

    private void checkNetwork() {
        boolean changedState = false;
        if (isNetworkUp()) {
            if (state.equals(State.DISCONNECTED) || state.equals(State.OFFLINE)) {
                setState(ConnectivityObservable.State.DISCONNECTED);
                changedState = true;
                service.reconnect();
            }
        } else {
            setState(ConnectivityObservable.State.DISCONNECTED);
            setState(ConnectivityObservable.State.OFFLINE);
            changedState = true;
        }
        if (!changedState && isWiFiUp())
            doNotify(null);
    }

    public boolean isNetworkUp() {
        final NetworkInfo activeNetworkInfo = ((ConnectivityManager) service.getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    public boolean isWiFiUp() {
        final NetworkInfo activeNetwork = ((ConnectivityManager)service.getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting()
                && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
    }
}
