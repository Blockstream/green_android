package com.greenaddress.greenapi;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.greenaddress.gdk.CodeResolver;
import com.greenaddress.greenapi.data.HWDeviceData;
import com.greenaddress.greenapi.data.PinData;
import com.greenaddress.greenbits.AuthenticationHandler;
import com.greenaddress.greenbits.ui.BuildConfig;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;
import com.greenaddress.greenbits.wallets.BTChipHWWallet;

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
    private String mWatchOnlyUsername;
    private boolean mLoginWithPin;
    private HWDeviceData mHWDevice;
    private HWWallet mHWWallet;
    private CodeResolver mHWResolver;
    private boolean pinJustSaved = false;

    public boolean isConnected() { return mState == ConnState.CONNECTED; }

    public boolean isPostLogin() { return mState == ConnState.POSTLOGGEDIN; }

    public boolean isOffline() { return mState == ConnState.OFFLINE; }

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

    public void goLoginRequired() {
        setState(ConnState.LOGINREQUIRED);
    }

    public boolean isLoginRequired() {
        return mState == ConnState.LOGINREQUIRED;
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

    public HWWallet getHWWallet() {
        return mHWWallet;
    }

    public String getWatchOnlyUsername() {
        return mWatchOnlyUsername;
    }

    public void connect(final Context context) throws Exception {
        final String network = PreferenceManager.getDefaultSharedPreferences(context).getString(PrefKeys.NETWORK_ID_ACTIVE, "mainnet");
        final SharedPreferences preferences = context.getSharedPreferences(network, MODE_PRIVATE);
        final String proxyHost = preferences.getString(PrefKeys.PROXY_HOST, "");
        final String proxyPort = preferences.getString(PrefKeys.PROXY_PORT, "");
        final Boolean proxyEnabled = preferences.getBoolean(PrefKeys.PROXY_ENABLED, false);
        final Boolean torEnabled = preferences.getBoolean(PrefKeys.TOR_ENABLED, false);
        pinJustSaved = false;

        String deviceId = preferences.getString(PrefKeys.DEVICE_ID, null);
        if (deviceId == null) {
            // Generate a unique device id
            deviceId = UUID.randomUUID().toString();
            preferences.edit().putString(PrefKeys.DEVICE_ID, deviceId).apply();
        }
        setState(ConnState.CONNECTING);
        final boolean isDebug = BuildConfig.DEBUG;
        Log.d(TAG,"connecting to " + network + (isDebug ? " in DEBUG mode" : "") + (torEnabled ? " with TOR" : ""));
        if (proxyEnabled || torEnabled) {
            final String proxyString;
            if (!proxyEnabled || TextUtils.isEmpty(proxyHost)) {
               proxyString = "";
            } else {
               proxyString = String.format(Locale.US, "%s:%s", proxyHost, proxyPort);
               Log.d(TAG, "connecting with proxy " + proxyString);
            }
            getSession().connectWithProxy(network, proxyString, torEnabled, isDebug);
        } else {
            getSession().connect(network, isDebug);
        }
        setState(ConnState.CONNECTED);
    }

    public void login(final Activity parent, final HWDeviceData hwDevice, final CodeResolver hwResolver, final HWWallet hwWallet) throws Exception {
        try {
            setState(ConnState.LOGGINGIN);
            mHWDevice = hwDevice;
            mHWResolver = hwResolver;
            mHWWallet = hwWallet;
            getSession().login(parent, hwDevice, "", "").resolve(null, hwResolver);
            setState(ConnState.LOGGEDIN);
        } catch (final Exception e) {
            Log.e(TAG, "Error while logging in " + e.getMessage() );
            setState(ConnState.CONNECTED);
            throw e;
        }
    }

    public void loginWithPin(final String pin, final PinData pinData) throws Exception {
        login(null,null, pin, pinData,null, null);
    }
    public void loginWatchOnly(final String username, final String password) throws Exception {
        login(null,null, null,null,username, password);
    }
    public void loginWithMnemonic(final String mnemonic, final String mnemonicPassword) throws Exception {
        login(mnemonic, mnemonicPassword, null, null, null, null);
    }

    public void login(final String mnenonic, final String mnemonicPassword,
                      final String pin, final PinData pinData,
                      final String username, final String password) throws Exception {
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
            setState(ConnState.LOGGEDIN);
        } catch (final Exception e) {
            Log.e(TAG, "Error while logging " + e.getMessage() );
            setState(ConnState.DISCONNECTED);
            throw e;
        }
    }

    public void disconnect() {
        setState(ConnState.DISCONNECTING);
        mWatchOnlyUsername = null;
        mLoginWithPin = false;
        mHWDevice = null;
        mHWResolver = null;
        try {
            getSession().disconnect();
            getSession().destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
