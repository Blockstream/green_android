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
        OFFLINE(0), DISCONNECTING(1), DISCONNECTED(2), CONNECTING(3), CONNECTED(4), LOGGINGIN(5), LOGGEDIN(6), POSTLOGGEDIN(7), LOGINREQUIRED(8);

        private final int value;

        ConnState(int value) {
            this.value = value;
        }
    }

    private ConnState mState;
    private GDKSession mSession;
    private String mWatchOnlyUsername;
    private String mNetwork;
    private String mProxyHost;
    private String mProxyPort;
    private boolean mProxyEnabled;
    private boolean mTorEnabled;
    private boolean mLoginWithPin;
    private Exception mLastLoginException = null;
    private HWDeviceData mHWDevice;
    private CodeResolver mHWResolver;

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

    public boolean isOffline() { return mState == ConnState.OFFLINE; }

    public boolean isLastLoginFailed() {
        return mLastLoginException != null;
    }

    public boolean isDisconnectedOrLess() {
        return mState.value <= ConnState.DISCONNECTED.value;
    }

    public boolean isDisconnected() {
        return mState == ConnState.DISCONNECTED;
    }

    public boolean isLoggingInOrMore() {
        return mState.value >= ConnState.LOGGINGIN.value;
    }

    public void goOffline() {
        setState(ConnState.OFFLINE);
    }

    public void goPostLogin() {
        setState(ConnState.POSTLOGGEDIN);
    }

    private void setState(ConnState state) {
        Log.d(TAG, "setting to " + state);
        this.mState = state;
        setChanged();
        notifyObservers();
    }

    public void clearPreviousLoginError() {
        if (isLastLoginFailed()) {
            Log.d(TAG, "clearing previous login error");
            mLastLoginException = null;
        }
    }

    public void goLoginRequired() {
        setState(ConnState.LOGINREQUIRED);
    }

    public boolean isLoginRequired() {
        return mState == ConnState.LOGINREQUIRED;
    }


    public Exception getLastLoginException() {
        return mLastLoginException;
    }

    public boolean isWatchOnly() {
        return mWatchOnlyUsername != null;
    }

    public boolean isHW() {
        return mHWDevice != null;
    }

    public boolean isLoginWithPin() {
        return mLoginWithPin;
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

    public  void connect() throws RuntimeException {
        setState(ConnState.CONNECTING);
        final boolean isDebug = BuildConfig.DEBUG;
        Log.d(TAG,"connecting to " + mNetwork + (isDebug ? " in DEBUG mode" : "") + (mTorEnabled ? " with TOR" : ""));
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
        setState(ConnState.CONNECTED);
    }

    public void login(final Activity parent, final HWDeviceData hwDevice, final CodeResolver hwResolver) {

        try {
            connect();
            setState(ConnState.LOGGINGIN);
            this.mHWDevice = hwDevice;
            this.mHWResolver = hwResolver;
            mSession.login(parent, hwDevice, "", "").resolve(null, hwResolver);
            mLastLoginException = null;
            setState(ConnState.LOGGEDIN);
        } catch (final Exception e) {
            Log.e(TAG, "Error while logging in " + e.getMessage() );
            mLastLoginException = e;
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
        try {
            connect();
            setState(ConnState.LOGGINGIN);
            final Activity parent = null; // FIXME: Pass this in/split this call up
            if (!TextUtils.isEmpty(mnenonic) && mnemonicPassword != null) {
                Log.d(TAG, "logging with mnemonic");
                mWatchOnlyUsername = null;
                mLoginWithPin = false;
                mSession.login(parent, null, mnenonic, mnemonicPassword).resolve(null, null);
            } else if (!TextUtils.isEmpty(pin) && pinData != null) {
                Log.d(TAG, "logging with pin");
                mWatchOnlyUsername = null;
                mLoginWithPin = true;
                mSession.loginWithPin(pin, pinData);
            } else if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
                Log.d(TAG, "logging watch only");
                mWatchOnlyUsername = username;
                mLoginWithPin = false;
                mSession.loginWatchOnly(username, password);
            } else {
                throw new Exception("wrong parameters");
            }
            mLastLoginException = null;
            setState(ConnState.LOGGEDIN);
        } catch (final Exception e) {
            Log.e(TAG, "Error while logging " + e.getMessage() );
            mLastLoginException = e;
            setState(ConnState.DISCONNECTED);
        }
    }


    public void disconnect() {
        setState(ConnState.DISCONNECTING);
        mWatchOnlyUsername = null;
        mLoginWithPin = false;
        mHWDevice = null;
        mHWResolver = null;
        mSession.disconnect();
        setState(ConnState.DISCONNECTED);
    }
}
