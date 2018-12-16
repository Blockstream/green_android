package com.greenaddress.greenapi;

import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;

import com.greenaddress.gdk.CodeResolver;
import com.greenaddress.gdk.GDKSession;
import com.greenaddress.greenapi.data.HWDeviceData;
import com.greenaddress.greenapi.data.PinData;
import com.greenaddress.greenbits.ui.BuildConfig;

import java.util.Locale;
import java.util.Observable;

public class ConnectionManager extends Observable {
    public static final String TAG = "STATE";

    private enum ConnState {
        OFFLINE(0), DISCONNECTING(1), DISCONNECTED(2), CONNECTING(3), CONNECTED(4), LOGGINGIN(5), LOGGEDIN(6), POSTLOGGEDIN(7);

        private final int value;

        ConnState(int value) {
            this.value = value;
        }
    }

    private ConnState mPreviousState;
    private ConnState mState;
    private GDKSession mSession;
    private String mWatchOnlyUsername;
    private String mNetwork;
    private String mProxyHost;
    private String mProxyPort;
    private boolean mProxyEnabled;
    private boolean mTorEnabled;
    private HWDeviceData mHWDevice;
    private CodeResolver mHWResolver;
    private static int CONNECTION_RETRY_ATTEMPTS = 3;
    private int mConnectionCounter = CONNECTION_RETRY_ATTEMPTS;

    public ConnectionManager(final GDKSession session, final String network,
                             final String proxyHost, final String proxyPort,
                             final boolean proxyEnabled, final boolean torEnabled) {
        this.mSession = session;
        this.mNetwork = network;
        this.mProxyHost = proxyHost;
        this.mProxyPort = proxyPort;
        this.mProxyEnabled = proxyEnabled;
        this.mTorEnabled = torEnabled;
        this.mState = ConnState.DISCONNECTED;
        this.mPreviousState = ConnState.DISCONNECTED;
    }

    public void resetAttempts() {
        mConnectionCounter = CONNECTION_RETRY_ATTEMPTS;
    }

    public void setTorEnabled(boolean mTorEnabled) {
        this.mTorEnabled = mTorEnabled;
    }

    public void setProxyHostAndPort(final String proxyHost, final String proxyPort) {
        this.mProxyHost = proxyHost;
        this.mProxyPort = proxyPort;
    }

    public void setProxyEnabled(final boolean proxyEnabled) {
        this.mProxyEnabled = proxyEnabled;
    }

    public void setNetwork(final String network) {
        this.mNetwork = network;
    }

    public boolean isLoggedIn() { return mState == ConnState.LOGGEDIN; }

    public boolean isConnected() { return mState == ConnState.CONNECTED; }

    public boolean isPostLogin() { return mState == ConnState.POSTLOGGEDIN; }

    public boolean isConnectedWasLoggingIn() {
        //means login failed
        return mState == ConnState.CONNECTED && mPreviousState == ConnState.LOGGINGIN;
    }

    public boolean isDisconnectedOrLess() {
        return mState.value <= ConnState.DISCONNECTED.value;
    }

    private boolean isDisconnected() {
        return mState == ConnState.DISCONNECTED;
    }

    public boolean isLoggingInOrMore() {
        return mState.value >= ConnState.LOGGINGIN.value;
    }

    public void goOffline() {
        setState(ConnState.OFFLINE);
    }

    public void goOnline() {
        if (mState == ConnState.OFFLINE) {
            setState(ConnState.DISCONNECTED);
        }
    }

    public void goPostLogin() {
        if(mState!=ConnState.LOGGEDIN) {
            //TODO just return?
            throw new RuntimeException("Going post login without being loggedin");
        }
        setState(ConnState.POSTLOGGEDIN);
    }

    private void setState(ConnState state) {
        Log.d(TAG, "setting to " + state);
        this.mPreviousState = this.mState;
        this.mState = state;
        setChanged();
        notifyObservers();
    }

    public void clearPreviousLoginError() {
        if (isConnectedWasLoggingIn()) {
            Log.d(TAG, "clearing previous login error");
            this.mPreviousState = ConnState.CONNECTED;
        }
    }

    public boolean isWatchOnly() {
        return mWatchOnlyUsername != null;
    }

    public boolean isHW() {
        return mHWDevice != null;
    }

    public CodeResolver getHWResolver() {
        return mHWResolver;
    }

    public HWDeviceData getHWDeviceData() {
        return mHWDevice;
    }

    public String getWatchOnlyUsername() {
        return mWatchOnlyUsername;
    }

    public synchronized void connect() {
        if(!isDisconnected()) {
            Log.w(TAG,"Calling connect from state " + mState + " doing nothing" );
            return;
        }

        setState(ConnState.CONNECTING);
        final boolean isDebug = BuildConfig.DEBUG;
        Log.d(TAG,"connecting to " + mNetwork + (isDebug ? " in DEBUG mode" : "") + (mTorEnabled ? " with TOR" : ""));
        try {
            if (mProxyEnabled || mTorEnabled) {
                final String proxyString;
                if (!mProxyEnabled || TextUtils.isEmpty(mProxyHost)) {
                   proxyString = "";
                } else {
                   proxyString = String.format(Locale.US, "%s:%s", mProxyHost, mProxyPort);
                   Log.d(TAG, "connecting with proxy " + proxyString);
                }
                mSession.connectWithProxy(mNetwork, proxyString, mTorEnabled, isDebug);
            } else {
                mSession.connect(mNetwork, isDebug);
            }
            resetAttempts();
            setState(ConnState.CONNECTED);
        } catch (final Exception e) {
            Log.i(TAG, "cannot connect " + e.getMessage());
            --mConnectionCounter;
            if (mConnectionCounter < 0) {
                Log.w(TAG, "Failed to connect for " + CONNECTION_RETRY_ATTEMPTS + " times, going offline");
                setState(ConnState.OFFLINE);
            } else {
                setState(ConnState.DISCONNECTED);
            }
        }
    }

    public void login(final Activity parent, final HWDeviceData hwDevice, final CodeResolver hwResolver) {
        setState(ConnState.LOGGINGIN);
        try {
            this.mHWDevice = hwDevice;
            this.mHWResolver = hwResolver;
            mSession.login(parent, hwDevice, "", "").resolve(null, hwResolver);
            setState(ConnState.LOGGEDIN);
        } catch (final Exception e) {
            Log.e(TAG, "Error while logging in " + e.getMessage() );
            setState(ConnState.CONNECTED);
        }
    }

    public void loginWithPin(final String pin, final PinData pinData) {
        login(null,null, pin, pinData,null, null);
    }
    public void loginWatchOnly(final String username, final String password) {
        login(null,null, null,null,username, password);
    }
    public void loginWithMnemonic(final String mnenonic, final String mnemonicPassword) {
        login(mnenonic, mnemonicPassword, null, null, null, null);
    }
    public void login(final String mnenonic, final String mnemonicPassword,
                      final String pin, final PinData pinData,
                      final String username, final String password) {
        setState(ConnState.LOGGINGIN);
        try {
            final Activity parent = null; // FIXME: Pass this in/split this call up
            if (!TextUtils.isEmpty(mnenonic) && mnemonicPassword != null) {
                Log.d(TAG, "logging with mnemonic");
                mWatchOnlyUsername = null;
                mSession.login(parent, null, mnenonic, mnemonicPassword).resolve(null, null);
            } else if (!TextUtils.isEmpty(pin) && pinData != null) {
                Log.d(TAG, "logging with pin");
                mWatchOnlyUsername = null;
                mSession.loginWithPin(pin, pinData);
            } else if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
                Log.d(TAG, "logging watch only");
                mWatchOnlyUsername = username;
                mSession.loginWatchOnly(username, password);
            } else {
                throw new Exception("wrong parameters");
            }
            setState(ConnState.LOGGEDIN);
        } catch (Exception e) {
            Log.e(TAG, "Error while logging " + e.getMessage() );
            setState(ConnState.CONNECTED);
        }
    }

    public void disconnect() {
        setState(ConnState.DISCONNECTING);
        mWatchOnlyUsername = null;
        mHWDevice = null;
        mHWResolver = null;
        mSession.disconnect();
        setState(ConnState.DISCONNECTED);
    }
}
