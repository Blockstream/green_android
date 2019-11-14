package com.greenaddress.greenapi;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.google.common.util.concurrent.ListenableFuture;
import com.greenaddress.gdk.CodeResolver;
import com.greenaddress.greenapi.data.HWDeviceData;
import com.greenaddress.greenapi.data.PinData;
import com.greenaddress.greenbits.AuthenticationHandler;
import com.greenaddress.greenbits.ui.BuildConfig;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;

import java.util.Locale;
import java.util.Observable;
import java.util.UUID;

import static android.content.Context.MODE_PRIVATE;
import static com.greenaddress.gdk.GDKSession.getSession;

public class ConnectionManager extends Observable {
    public static final String TAG = "STATE";


    private enum ConnState {
        OFFLINE(0), DISCONNECTING(1), DISCONNECTED(2), CONNECTING(3), CONNECTED(4), LOGGINGIN(5), LOGGEDIN(6), POSTLOGGEDIN(7), LOGINREQUIRED(8);

        private final int value;

        ConnState(int value) {
            this.value = value;
        }
    }

    private ConnState mState = ConnState.DISCONNECTED;
    public String mWatchOnlyUsername;
    public String mNetwork;
    public String mProxyHost;
    public String mProxyPort;
    public boolean mProxyEnabled;
    public boolean mTorEnabled;
    public boolean mLoginWithPin;
    public Exception mLastLoginException = null;
    public HWDeviceData mHWDevice;
    public CodeResolver mHWResolver;
    private boolean pinJustSaved = false;

    public ConnectionManager(final String network) {
        this.mNetwork = network;
    }
    public ConnectionManager(final String network,
                             final String proxyHost, final String proxyPort,
                             final boolean proxyEnabled, final boolean torEnabled) {
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

    public void connect(final Context context) throws RuntimeException {
        mNetwork = PreferenceManager.getDefaultSharedPreferences(context).getString(PrefKeys.NETWORK_ID_ACTIVE, "mainnet");
        final SharedPreferences preferences = context.getSharedPreferences(mNetwork, MODE_PRIVATE);
        mProxyHost = preferences.getString(PrefKeys.PROXY_HOST, "");
        mProxyPort = preferences.getString(PrefKeys.PROXY_PORT, "");
        mProxyEnabled = preferences.getBoolean(PrefKeys.PROXY_ENABLED, false);
        mTorEnabled = preferences.getBoolean(PrefKeys.TOR_ENABLED, false);
        pinJustSaved = false;

        String deviceId = preferences.getString(PrefKeys.DEVICE_ID, null);
        if (deviceId == null) {
            // Generate a unique device id
            deviceId = UUID.randomUUID().toString();
            preferences.edit().putString(PrefKeys.DEVICE_ID, deviceId).apply();
        }
        connect();
    }

    private void connect() throws RuntimeException {
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
            getSession().connectWithProxy(mNetwork, proxyString, mTorEnabled, isDebug);
        } else {
            getSession().connect(mNetwork, isDebug);
        }
        setState(ConnState.CONNECTED);
    }

    public void login(final Activity parent, final HWDeviceData hwDevice, final CodeResolver hwResolver) {

        try {
            setState(ConnState.LOGGINGIN);
            this.mHWDevice = hwDevice;
            this.mHWResolver = hwResolver;
            getSession().login(parent, hwDevice, "", "").resolve(null, hwResolver);
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
    public void loginWithMnemonic(final String mnemonic, final String mnemonicPassword) {
        login(mnemonic, mnemonicPassword, null, null, null, null);
    }
    public void login(final String mnenonic, final String mnemonicPassword,
                      final String pin, final PinData pinData,
                      final String username, final String password) {
        try {
            setState(ConnState.LOGGINGIN);
            final Activity parent = null; // FIXME: Pass this in/split this call up
            if (!TextUtils.isEmpty(mnenonic) && mnemonicPassword != null) {
                Log.d(TAG, "logging with mnemonic");
                mWatchOnlyUsername = null;
                mLoginWithPin = false;
                getSession().login(parent, null, mnenonic, mnemonicPassword).resolve(null, null);
            } else if (!TextUtils.isEmpty(pin) && pinData != null) {
                Log.d(TAG, "logging with pin");
                mWatchOnlyUsername = null;
                mLoginWithPin = true;
                getSession().loginWithPin(pin, pinData);
            } else if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
                Log.d(TAG, "logging watch only");
                mWatchOnlyUsername = username;
                mLoginWithPin = false;
                getSession().loginWatchOnly(username, password);
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
        getSession().disconnect();
        setState(ConnState.DISCONNECTED);
    }

    public void setPin(final String mnemonic, final String pin, final SharedPreferences preferences) throws Exception {
        final PinData pinData = getSession().setPin(mnemonic, pin, "default");
        AuthenticationHandler.setPin(pinData, pin.length() == 6, preferences);
        setPinJustSaved(true);
    }

    public boolean isPinJustSaved() {
        return pinJustSaved;
    }

    public void setPinJustSaved(boolean pinJustSaved) {
        this.pinJustSaved = pinJustSaved;
    }
}
